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
package net.gcolin.simplerepo.servlet;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import net.gcolin.server.maven.IndexListener;
import net.gcolin.server.maven.PluginContainer;
import net.gcolin.server.maven.PluginListener;
import net.gcolin.server.maven.RepositoryListener;
import net.gcolin.simplerepo.jmx.ConfigurationJmx;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.JmxUtil;

/**
 * Register plugins.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class StartListener implements ServletContextListener, PluginListener, PluginContainer {

  /**
   * Logger.
   */
  public static final transient Logger LOG = Logger.getLogger("net.gcolin.simplerepo.servlet");
  private List<PluginListener> pluginListeners = new ArrayList<>();
  private List<EventListener> allListeners = new ArrayList<EventListener>();
  private Map<File, ClassLoader> pluginClassLoaders = new HashMap<>();
  private ServletContext context;

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    context = sce.getServletContext();
    ConfigurationManager configManager =
        new ConfigurationManager((String) sce.getServletContext().getAttribute("contextName"));
    JmxUtil.publish(configManager.getConfigurationJmxName(), configManager, ConfigurationJmx.class);
    sce.getServletContext().setAttribute("configManager", configManager);
    File plugins = new File(configManager.getRoot(), "plugins");
    plugins.mkdirs();
    sce.getServletContext().setAttribute("pluginContainer", this);
    context.setAttribute("pluginsClassLoaders", Collections.emptyList());
    context.setAttribute("pluginListeners", Collections.emptyList());
    final File[] children = plugins.listFiles();
    if (children != null) {
      for (int i = 0; i < children.length; i++) {
        installPlugin(children[i]);
      }
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    for (int i = 0; i < allListeners.size(); i++) {
      EventListener event = allListeners.get(i);
      if (event instanceof ServletContextListener) {
        ((ServletContextListener) event).contextDestroyed(sce);
      }
    }

    ConfigurationManager configManager =
        (ConfigurationManager) sce.getServletContext().getAttribute("configManager");
    JmxUtil.unpublish(configManager.getConfigurationJmxName());
    for (Repository repository : configManager.getConfiguration().getRepositories()) {
      JmxUtil.unpublish(configManager.getRepositoryJmxName(repository));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see net.gcolin.simplerepo.servlet.PluginContainer#add(net.gcolin.server.maven.PluginListener)
   */
  @Override
  public synchronized void add(PluginListener pluginListener) {
    pluginListeners.add(pluginListener);
  }

  /*
   * (non-Javadoc)
   * 
   * @see net.gcolin.simplerepo.servlet.PluginContainer#installPlugin(java.io.File)
   */
  @Override
  public synchronized void installPlugin(final File file) {
    try {
      final URL url = file.toURI().toURL();
      ClassLoader pluginClassLoader =
          AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
              return new URLClassLoader(new URL[] {url}, StartListener.class.getClassLoader());
            }
          });
      pluginClassLoaders.put(file, pluginClassLoader);
      context.setAttribute("pluginsClassLoaders",
          Collections.unmodifiableCollection(pluginClassLoaders.values()));

      for (EventListener event : ServiceLoader.load(EventListener.class, pluginClassLoader)) {
        if (event.getClass().getClassLoader() == pluginClassLoader) {
          if (event instanceof ServletContextListener) {
            ((ServletContextListener) event).contextInitialized(new ServletContextEvent(context));
            allListeners.add(event);
          } else if (event instanceof PluginListener) {
            add((PluginListener) event);
          } else if (event instanceof RepositoryListener || event instanceof IndexListener) {
            allListeners.add(event);
          } else {
            LOG.warning("Plugins only supports javax.servlet.ServletContextListener, "
                + "net.gcolin.simplerepo.servlet.IndexListener,"
                + "net.gcolin.simplerepo.servlet.PluginListener"
                + "and net.gcolin.simplerepo.servlet.RepositoryListener");
          }
        }
      }

      context.setAttribute("pluginListeners", Collections.unmodifiableList(allListeners));

      onPluginInstalled(pluginClassLoader);

    } catch (MalformedURLException ex) {
      LOG.log(Level.SEVERE, "cannot load plugin " + file.getName(), ex);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see net.gcolin.simplerepo.servlet.PluginContainer#removePlugin(java.io.File)
   */
  @Override
  public synchronized void removePlugin(File file) {
    ClassLoader cl = pluginClassLoaders.remove(file);
    if (cl != null) {
      for (int i = allListeners.size() - 1; i >= 0; i--) {
        if (allListeners.get(i).getClass().getClassLoader() == cl) {
          allListeners.remove(i);
        }
      }
      for (int i = pluginListeners.size() - 1; i >= 0; i--) {
        if (pluginListeners.get(i).getClass().getClassLoader() == cl) {
          pluginListeners.remove(i);
        }
      }
      onPluginRemoved(cl);
    }
  }

  @Override
  public synchronized void onPluginInstalled(ClassLoader classLoader) {
    for (int i = 0; i < pluginListeners.size(); i++) {
      pluginListeners.get(i).onPluginInstalled(classLoader);
    }
  }

  @Override
  public synchronized void onPluginRemoved(ClassLoader classLoader) {
    for (int i = 0; i < pluginListeners.size(); i++) {
      pluginListeners.get(i).onPluginRemoved(classLoader);
    }
  }

}
