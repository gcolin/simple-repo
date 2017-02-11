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

import net.gcolin.simplerepo.IndexListener;
import net.gcolin.simplerepo.PluginContainer;
import net.gcolin.simplerepo.PluginListener;
import net.gcolin.simplerepo.RepositoryListener;
import net.gcolin.simplerepo.jmx.ConfigurationJmx;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.JmxUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

/**
 * Register plugins.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class StartListener
    implements ServletContextListener, PluginListener, PluginContainer, ServletRequestListener {

  /**
   * Logger.
   */
  public static final transient Logger LOG = Logger.getLogger("net.gcolin.simplerepo.servlet");
  private List<PluginListener> pluginListeners = new ArrayList<>();
  private List<EventListener> allListeners = new ArrayList<EventListener>();
  private ServletContext context;
  private ConfigurationManager configManager;
  private Map<String, Properties> plugins = new HashMap<>();
  private Set<String> activePlugins = new HashSet<>();

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    context = sce.getServletContext();
    configManager =
        new ConfigurationManager((String) sce.getServletContext().getAttribute("contextName"));
    JmxUtil.publish(configManager.getConfigurationJmxName(), configManager, ConfigurationJmx.class);
    sce.getServletContext().setAttribute("configManager", configManager);
    sce.getServletContext().setAttribute("pluginContainer", this);
    context.setAttribute("pluginListeners", Collections.unmodifiableList(allListeners));
    String activePlugins = configManager.getProperty("plugins");
    Set<String> actives = null;
    if (activePlugins != null) {
      if (actives == null) {
        actives = new HashSet<>();
      }
      for (String activePlugin : activePlugins.split(",")) {
        if (activePlugin.trim().isEmpty()) {
          continue;
        }
        actives.add(activePlugin.trim());
      }
    }
    try {
      Enumeration<URL> allPlugins =
          this.getClass().getClassLoader().getResources("META-INF/plugin.properties");
      while (allPlugins.hasMoreElements()) {
        Properties props = new Properties();
        try (InputStream in = allPlugins.nextElement().openStream()) {
          props.load(in);
        }
        plugins.put(props.getProperty("name"), props);
        if (actives == null || actives.contains(props.getProperty("name"))) {
          installPlugin(props);
        }
      }
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
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
   * @see net.gcolin.simplerepo.servlet.PluginContainer#add(net.gcolin.server.maven .PluginListener)
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
  public synchronized void installPlugin(Properties props) {
    try {
      if (!activePlugins.add(props.getProperty("name"))) {
        return;
      }

      String events = props.getProperty("event");
      if (events == null) {
        throw new IllegalArgumentException(
            props.getProperty("name") + " plugin must have an 'event' property");
      }

      for (String evtClass : events.split(",")) {
        EventListener event = (EventListener) this.getClass().getClassLoader()
            .loadClass(evtClass.trim()).newInstance();
        boolean used = false;
        if (event instanceof ServletContextListener) {
          ((ServletContextListener) event).contextInitialized(new ServletContextEvent(context));
          used = true;
        }
        if (event instanceof PluginListener) {
          add((PluginListener) event);
          used = true;
        }
        if (event instanceof RepositoryListener || event instanceof IndexListener) {
          used = true;
        }
        if (!used) {
          LOG.warning("Plugins only supports javax.servlet.ServletContextListener, "
              + "net.gcolin.simplerepo.servlet.IndexListener,"
              + "net.gcolin.simplerepo.servlet.PluginListener"
              + "and net.gcolin.simplerepo.servlet.RepositoryListener");
        } else {
          allListeners.add(event);
        }
      }
      configManager.setProperty("plugins", getActivePluginString());
      onPluginInstalled();

    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
      LOG.log(Level.SEVERE, "cannot load plugin " + props.getProperty("name"), ex);
    }
  }

  private String getActivePluginString() {
    StringBuilder str = new StringBuilder();
    for (String plugin : activePlugins) {
      if (str.length() > 0) {
        str.append(',');
      }
      str.append(plugin);
    }
    return str.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see net.gcolin.simplerepo.servlet.PluginContainer#removePlugin(java.io.File)
   */
  @Override
  public synchronized void removePlugin(Properties props) {
    if (!activePlugins.remove(props.getProperty("name"))) {
      return;
    }
    String events = props.getProperty("event");
    if (events == null) {
      throw new IllegalArgumentException(
          props.getProperty("name") + " plugin must have an 'event' property");
    }
    for (String evtClass : events.split(",")) {
      String eventClass = evtClass.trim();
      for (int i = allListeners.size() - 1; i >= 0; i--) {
        if (allListeners.get(i).getClass().getName().equals(eventClass)) {
          if (allListeners.get(i) instanceof ServletContextListener) {
            ((ServletContextListener) allListeners.get(i))
                .contextDestroyed(new ServletContextEvent(context));
          }
          allListeners.remove(i);
        }
      }
      for (int i = pluginListeners.size() - 1; i >= 0; i--) {
        if (pluginListeners.get(i).getClass().getName().equals(eventClass)) {
          pluginListeners.remove(i);
        }
      }
    }

    configManager.setProperty("plugins", getActivePluginString());
    onPluginRemoved();
  }

  @Override
  public synchronized void onPluginInstalled() {
    for (int i = 0; i < pluginListeners.size(); i++) {
      pluginListeners.get(i).onPluginInstalled();
    }
  }

  @Override
  public synchronized void onPluginRemoved() {
    for (int i = 0; i < pluginListeners.size(); i++) {
      pluginListeners.get(i).onPluginRemoved();
    }
  }

  @Override
  public Map<String, Properties> getPlugins() {
    return plugins;
  }

  @Override
  public Collection<String> getActivePlugins() {
    return activePlugins;
  }

  @Override
  public void onPluginUpdated(String name) {
    for (int i = 0; i < pluginListeners.size(); i++) {
      pluginListeners.get(i).onPluginUpdated(name);
    }
  }

  @Override
  public void requestDestroyed(ServletRequestEvent sre) {}

  @Override
  public void requestInitialized(ServletRequestEvent sre) {
    if (configManager.getServerBaseUrl() == null) {
      configManager.setServerBaseUrl("http://" + sre.getServletRequest().getServerName() + ":"
          + sre.getServletRequest().getServerPort()
          + sre.getServletRequest().getServletContext().getContextPath() + "/");
    }

  }

}
