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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationJmx;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.JmxUtil;

/**
 * Register plugins.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class StartListener
    implements ServletContextListener, ServletRequestListener {

  private ConfigurationManager configManager;

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
	 configManager =
        new ConfigurationManager((String) sce.getServletContext().getAttribute("contextName"));
    JmxUtil.publish(configManager.getConfigurationJmxName(), configManager, ConfigurationJmx.class);
    sce.getServletContext().setAttribute("configManager", configManager);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    JmxUtil.unpublish(configManager.getConfigurationJmxName());
    for (Repository repository : configManager.getConfiguration().getRepositories()) {
      JmxUtil.unpublish(configManager.getRepositoryJmxName(repository));
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
