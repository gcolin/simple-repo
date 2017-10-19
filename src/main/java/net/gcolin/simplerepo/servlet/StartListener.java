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

import org.apache.commons.io.IOUtils;

import net.gcolin.simplerepo.business.ConfigurationManager;
import net.gcolin.simplerepo.business.JmxUtil;
import net.gcolin.simplerepo.business.SearchManager;
import net.gcolin.simplerepo.business.UpdateIndexManager;
import net.gcolin.simplerepo.jmx.ConfigurationJmx;
import net.gcolin.simplerepo.model.Repository;

/**
 * Create configuration.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class StartListener
        implements ServletContextListener {

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ConfigurationManager configManager
                = new ConfigurationManager((String) sce.getServletContext().getAttribute("contextName"));
        JmxUtil.publish(configManager.getConfigurationJmxName(), configManager, ConfigurationJmx.class);
        sce.getServletContext().setAttribute("configManager", configManager);
        sce.getServletContext().setAttribute("searchManager", new SearchManager(sce.getServletContext()));
        sce.getServletContext().setAttribute("updateManager", new UpdateIndexManager(sce.getServletContext()));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ConfigurationManager configManager = (ConfigurationManager) sce.getServletContext().getAttribute("configManager");
        JmxUtil.unpublish(configManager.getConfigurationJmxName());
        for (Repository repository : configManager.getConfiguration().getRepositories()) {
            JmxUtil.unpublish(configManager.getRepositoryJmxName(repository));
        }
        SearchManager searchManager = (SearchManager) sce.getServletContext().getAttribute("searchManager");
        IOUtils.closeQuietly(searchManager);
    }

}
