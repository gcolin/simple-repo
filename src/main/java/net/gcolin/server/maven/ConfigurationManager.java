/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.gcolin.server.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Starting of JMX name for a repository.
     */
    public static final String JMX_REPOSITORY
            = "net.gcolin.server.maven:type=Repository,name=";
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

    /**
     * Create a ConfigurationManager.
     */
    public ConfigurationManager() {
        String rootPath = System.getProperty("simplerepo.root");
        if (rootPath == null) {
            File userDir = new File(System.getProperty("user.home"));
            root = new File(userDir, ".simplerepo");
        } else {
            root = new File(rootPath);
        }

        if (root.mkdirs()) {
            Logger.getLogger(this.getClass().getName()).log(Level.FINE,
                    "create directory {0}", root);
        }

        try {
            ctx = JAXBContext.newInstance(Configuration.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
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

    /**
     * Re index the repositories.
     */
    private void loadMap() {
        Map<String, Repository> r = new HashMap<String, Repository>();
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
        config.setRepositories(new ArrayList<Repository>());
        Repository r = new Repository();
        r.setName("snapshots");
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
        r.setIncludes(Arrays.asList(
                "snapshots", "releases", "thirdparty", "central"));
        config.getRepositories().add(r);
        save();
    }

    /**
     * The root folder where are stored the configuration and the repositories.
     *
     * @return the root folder
     */
    public final File getRoot() {
        return root;
    }

    /**
     * Get the configuration.
     *
     * @return the configuration
     */
    public final Configuration getConfiguration() {
        return config;
    }

    /**
     * Save the configuration.
     */
    public final void save() {
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
    public final Repository getRepository(final String name) {
        return repos.get(name);
    }

    @Override
    public final void newRepository(final String name) {
        if (repos.containsKey(name)) {
            throw new IllegalArgumentException("A repository with the name "
                    + name + " already exists.");
        }
        lock.lock();
        try {
            Repository repo = new Repository();
            repo.setName(name);
            List<Repository> list
                    = new ArrayList<Repository>(config.getRepositories());
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
    private void handle(final Repository repository) {
        RepoHandle handle = new RepoHandle();
        handle.repo = repository;
        JmxUtil.publish(JMX_REPOSITORY
                + repository.getName(), handle, RepositoryJmx.class);
    }

    @Override
    public final int getMaxSnapshots() {
        return config.getMaxSnapshots();
    }

    @Override
    public final void setMaxSnapshots(final int max) {
        lock.lock();
        try {
            config.setMaxSnapshots(max);
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
        public final void rename(final String name) {
            if (name.equals(repo.getName())) {
                throw new IllegalArgumentException(
                        "This is the actual name of the repository");
            }
            Repository other = repos.get(name);
            if (other != null && other != repo) {
                throw new IllegalArgumentException(
                        "A repository with the name "
                        + name
                        + " already exists.");
            }
            lock.lock();
            try {
                String oldName = repo.getName();
                JmxUtil.unpublish(JMX_REPOSITORY + oldName);
                repo.setName(name);
                JmxUtil.publish(JMX_REPOSITORY + repo.getName(), this,
                        RepositoryJmx.class);
                for (Repository repository : config.getRepositories()) {
                    if (repository.getIncludes() != null
                            && repository.getIncludes().contains(oldName)) {
                        List<String> list
                                = new ArrayList<String>(
                                        repository.getIncludes());
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
        public final void updateRemoteUrl(final String url) {
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
        public final void addInclude(final String name) {
            if (name.equals(repo.getName())) {
                throw new IllegalArgumentException(
                        "Cannot include into itself");
            }
            Repository other = repos.get(name);
            if (other == null) {
                throw new IllegalArgumentException(
                        "The repository with the name "
                        + name
                        + " does not exists.");
            }
            lock.lock();
            try {
                other.setRemote(null);
                if (repo.getIncludes() == null) {
                    repo.setIncludes(Arrays.asList(other.getName()));
                } else {
                    List<String> list
                            = new ArrayList<String>(repo.getIncludes());
                    list.add(other.getName());
                    repo.setIncludes(list);
                }
                save();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public final void removeInclude(final String name) {
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
        public final void remove() {
            JmxUtil.unpublish(JMX_REPOSITORY + repo.getName());
            lock.lock();
            try {
                List<Repository> newrepos = new ArrayList<Repository>();
                for (Repository repository : config.getRepositories()) {
                    if (repository == repo) {
                        continue;
                    } else if (repository.getIncludes() != null
                            && repository.getIncludes()
                                    .contains(repo.getName())) {
                        List<String> list
                                = new ArrayList<String>(
                                        repository.getIncludes());
                        list.remove(repo.getName());
                        repository.setIncludes(list);
                    }
                    newrepos.add(repository);
                }
                config.setRepositories(newrepos);
                save();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public final String[] getIncludes() {
            List<String> includes = repo.getIncludes();
            if (includes == null) {
                return null;
            } else {
                return includes.toArray(new String[includes.size()]);
            }
        }

        @Override
        public final String getName() {
            return repo.getName();
        }

        @Override
        public final String getRemoteUrl() {
            return repo.getRemote();
        }

    }

    /**
     * @return the currentRetrieve
     */
    public final String getCurrentRetrieve() {
        return currentRetrieve;
    }

    /**
     * @param current the currentRetrieve to set
     */
    public final void setCurrentRetrieve(final String current) {
        this.currentRetrieve = current;
    }

}
