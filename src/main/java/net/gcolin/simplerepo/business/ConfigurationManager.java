/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.business;

import net.gcolin.simplerepo.jmx.ConfigurationJmx;
import net.gcolin.simplerepo.jmx.RepositoryJmx;
import net.gcolin.simplerepo.model.Configuration;
import net.gcolin.simplerepo.model.Repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Manage configuration.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class ConfigurationManager implements ConfigurationJmx {

  private Set<String> reserved = new HashSet<>();
  /**
   * JAXBContext for the Configuration.
   */
  private JAXBContext ctx;
  /**
   * The Configuration.
   */
  private Configuration config;
  /**
   * The root folder where are stored the configuration and the repositories.
   */
  private File root;
  /**
   * The repositories indexed.
   */
  private Map<String, Repository> repos;
  /**
   * The configuration modification lock.
   */
  private ReentrantLock lock = new ReentrantLock();
  /**
   * The absolute path of the current written file.
   */
  private volatile String currentRetrieve;

  private String contextName;

  private Properties properties;

  private volatile String currentAction;

  private String serverBaseUrl;

  /**
   * Create a ConfigurationManager.
   *
   * @param contextName contextName
   */
  public ConfigurationManager(String contextName) {
    reserved.add("config.xml");
    reserved.add("config.properties");
    reserved.add("plugins");
    this.contextName = contextName;
    String rootPath = System.getProperty("simplerepo.root");
    if (rootPath == null) {
      File userDir = new File(System.getProperty("user.home"));
      root = new File(userDir, ".simplerepo");
    } else {
      root = new File(rootPath);
    }

    if (root.mkdirs()) {
      Logger.getLogger(this.getClass().getName()).log(Level.FINE, "create directory {0}", root);
    }

    try {
      ctx = JAXBContext.newInstance(Configuration.class);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }

    File prop = new File(root, "config.properties");
    properties = new Properties();
    if (prop.exists()) {
      try (InputStream inStream = new FileInputStream(prop)) {
        properties.load(inStream);
      } catch (IOException ex) {
        throw new IllegalStateException(ex);
      }
    }

    File f = new File(root, "config.xml");
    if (f.exists()) {
      try {
        config = (Configuration) ctx.createUnmarshaller().unmarshal(f);
      } catch (JAXBException e) {
        loadDefaults();
      }
    } else {
      loadDefaults();
    }
    for (Repository repo : config.getRepositories()) {
      handle(repo);
    }
    loadMap();
  }
  
  public Collection<Repository> getRepositories() {
      return repos.values();
  }

  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  public void setProperty(String key, String value) {
    lock.lock();
    try (OutputStream out = new FileOutputStream(new File(root, "config.properties"))) {
      properties.setProperty(key, value);
      properties.store(out, "simple-repo configuration properties");
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    } finally {
      lock.unlock();
    }
  }


  /**
   * Get the JMX name of the configuration.
   *
   * @return the JMX name of the configuration
   */
  public String getConfigurationJmxName() {
    if (contextName == null) {
      return "net.gcolin.simplerepo:type=Configuration";
    } else {
      return "net.gcolin.simplerepo:ctx=" + contextName + ",type=Configuration";
    }
  }

  public String getContextName() {
    return contextName;
  }

  /**
   * Get the JMX name of a repository.
   *
   * @param repository repository
   * @return the JMX name of a repository
   */
  public String getRepositoryJmxName(Repository repository) {
    if (contextName == null) {
      return "net.gcolin.simplerepo:type=Repository,name=" + repository.getName();
    } else {
      return "net.gcolin.simplerepo:ctx=" + contextName + ",type=Repository,name="
          + repository.getName();
    }
  }

  /**
   * Re index the repositories.
   */
  private void loadMap() {
    Map<String, Repository> r = new TreeMap<>();
    for (Repository repo : config.getRepositories()) {
      r.put(repo.getName(), repo);
    }
    repos = r;
  }

  /**
   * Load default configuration.
   */
  private void loadDefaults() {
    config = new Configuration();
    config.setRepositories(new ArrayList<>());
    Repository r = new Repository();
    r.setName("snapshots");
    r.setArtifactMaxAge(TimeUnit.DAYS.toMillis(1L));
    config.getRepositories().add(r);
    r = new Repository();
    r.setName("releases");
    config.getRepositories().add(r);
    r = new Repository();
    r.setName("thirdparty");
    config.getRepositories().add(r);
    r = new Repository();
    r.setName("apache-snapshots");
    r.setRemote("https://repository.apache.org/snapshots/");
    config.getRepositories().add(r);
    r = new Repository();
    r.setName("central");
    r.setRemote("https://repo1.maven.org/maven2/");
    config.getRepositories().add(r);
    r = new Repository();
    r.setName("public");
    r.setIncludes(Arrays.asList("snapshots", "releases", "thirdparty", "central"));
    config.getRepositories().add(r);
    save();
  }

  /**
   * The root folder where are stored the configuration and the repositories.
   *
   * @return the root folder
   */
  public File getRoot() {
    return root;
  }

  /**
   * Get the configuration.
   *
   * @return the configuration
   */
  public Configuration getConfiguration() {
    return config;
  }

  /**
   * Save the configuration.
   */
  public void save() {
    try {
      Marshaller m = ctx.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      m.marshal(config, new File(root, "config.xml"));
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Get a repository by name.
   *
   * @param name repository name
   * @return null or a repository
   */
  public Repository getRepository(final String name) {
    return repos.get(name);
  }

  @Override
  public void newRepository(final String name) {
    if (reserved.contains(name)) {
      throw new IllegalArgumentException("This name is reserved");
    }
    if (repos.containsKey(name)) {
      throw new IllegalArgumentException("A repository with the name " + name + " already exists.");
    }
    lock.lock();
    try {
      Repository repo = new Repository();
      repo.setName(name);
      List<Repository> list = new ArrayList<Repository>(config.getRepositories());
      list.add(repo);
      config.setRepositories(list);
      handle(repo);
      loadMap();
      save();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Bind the repository to JMX.
   *
   * @param repository repository
   */
  private void handle(Repository repository) {
    JmxUtil.publish(getRepositoryJmxName(repository), toHandle(repository), RepositoryJmx.class);
  }
  
  public RepoHandle toHandle(Repository repository) {
    RepoHandle handle = new RepoHandle();
    handle.repo = repository;
    return handle;
  }

  public Map<String, Repository> getRepos() {
    return repos;
  }

  @Override
  public int getMaxSnapshots() {
    return config.getMaxSnapshots();
  }

  @Override
  public void setMaxSnapshots(final int max) {
    lock.lock();
    try {
      config.setMaxSnapshots(max);
      save();
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public String getElasticsearch() {
    return config.getElasticsearch();
  }

  @Override
  public void setElasticsearch(String elasticsearch) {
    lock.lock();
    try {
      config.setElasticsearch(elasticsearch);
      save();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long getNotFoundCache() {
    return config.getNotFoundCache();
  }

  @Override
  public void setNotFoundCache(long notfoundcache) {
    lock.lock();
    try {
      config.setNotFoundCache(notfoundcache);
      save();
    } finally {
      lock.unlock();
    }
  }
  
  @Override
  public String getTheme() {
    return config.getTheme();
  }
  
  @Override
  public void setTheme(String theme) {
    lock.lock();
    try {
      config.setTheme(theme);
      save();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Implements RepositoryJmx.
   */
  public class RepoHandle implements RepositoryJmx {

    /**
     * Linked repository.
     */
    private Repository repo;

    @Override
    public void rename(String name) {
      if (reserved.contains(name)) {
        throw new IllegalArgumentException("This name is reserved");
      }
      if (name.equals(repo.getName())) {
        throw new IllegalArgumentException("This is the actual name of the repository");
      }
      Repository other = repos.get(name);
      if (other != null && other != repo) {
        throw new IllegalArgumentException(
            "A repository with the name " + name + " already exists.");
      }
      lock.lock();
      try {
        String oldName = repo.getName();
        JmxUtil.unpublish(getRepositoryJmxName(repo));
        repo.setName(name);
        JmxUtil.publish(getRepositoryJmxName(repo), this, RepositoryJmx.class);
        for (Repository repository : config.getRepositories()) {
          if (repository.getIncludes() != null && repository.getIncludes().contains(oldName)) {
            List<String> list = new ArrayList<String>(repository.getIncludes());
            list.add(name);
            repository.setIncludes(list);
          }
        }
        loadMap();
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void updateRemoteUrl(String url) {
      lock.lock();
      try {
        if (url == null || url.trim().isEmpty()) {
          repo.setRemote(null);
        } else {
          repo.setRemote(url);
          repo.setIncludes(null);
        }
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void addInclude(String name) {
      if (name.equals(repo.getName())) {
        throw new IllegalArgumentException("Cannot include into itself");
      }
      Repository other = repos.get(name);
      if (other == null) {
        throw new IllegalArgumentException(
            "The repository with the name " + name + " does not exists.");
      }
      lock.lock();
      try {
        other.setRemote(null);
        if (repo.getIncludes() == null) {
          repo.setIncludes(Arrays.asList(other.getName()));
        } else {
          List<String> list = new ArrayList<String>(repo.getIncludes());
          list.add(other.getName());
          repo.setIncludes(list);
        }
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void removeInclude(String name) {
      lock.lock();
      try {
        List<String> list = new ArrayList<String>(repo.getIncludes());
        list.remove(name);
        repo.setIncludes(list);
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void remove() {
      JmxUtil.unpublish(getRepositoryJmxName(repo));
      lock.lock();
      try {
        List<Repository> newrepos = new ArrayList<Repository>();
        for (Repository repository : config.getRepositories()) {
          if (repository == repo) {
            continue;
          } else if (repository.getIncludes() != null
              && repository.getIncludes().contains(repo.getName())) {
            List<String> list = new ArrayList<String>(repository.getIncludes());
            list.remove(repo.getName());
            repository.setIncludes(list);
          }
          newrepos.add(repository);
        }
        config.setRepositories(newrepos);
        loadMap();
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public String[] getIncludes() {
      List<String> includes = repo.getIncludes();
      if (includes == null) {
        return new String[0];
      } else {
        return includes.toArray(new String[includes.size()]);
      }
    }

    @Override
    public String getName() {
      return repo.getName();
    }

    @Override
    public String getRemoteUrl() {
      return repo.getRemote();
    }

    @Override
    public void updateArtifactMaxAge(long milliseconds) {
      lock.lock();
      try {
        repo.setArtifactMaxAge(milliseconds);
        save();
      } finally {
        lock.unlock();
      }
    }

    @Override
    public long getArtifactMaxAge() {
      return repo.getArtifactMaxAge();
    }

  }

  /**
   * @return the currentRetrieve
   */
  public String getCurrentRetrieve() {
    return currentRetrieve;
  }

  /**
   * @param currentRetrieve the currentRetrieve to set
   */
  public void setCurrentRetrieve(String currentRetrieve) {
    this.currentRetrieve = currentRetrieve;
  }

  public String getCurrentAction() {
    return currentAction;
  }

  public void setCurrentAction(String currentAction) {
    this.currentAction = currentAction;
  }

  public String getServerBaseUrl() {
    return serverBaseUrl;
  }

  public void setServerBaseUrl(String serverBaseUrl) {
    this.serverBaseUrl = serverBaseUrl;
  }

}
