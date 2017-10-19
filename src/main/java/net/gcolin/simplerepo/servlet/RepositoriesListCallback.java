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
package net.gcolin.simplerepo.servlet;

import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.model.Configuration;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import net.gcolin.simplerepo.business.ConfigurationManager;

/**
 * List all repositories.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepositoriesListCallback extends ListCallback {

    /**
     * The configuration manager.
     */
    private final ConfigurationManager configurationManager;

    /**
     * Create AllRepositoriesListCallback.
     *
     * @param configManager configurationManager
     */
    public RepositoriesListCallback(ConfigurationManager configManager) {
        this.configurationManager = configManager;
    }

    @Override
    public void fillTable(Writer writer) throws IOException {
        final Configuration config = configurationManager.getConfiguration();
        for (Repository r : config.getRepositories()) {
            writer.write("<tr><td><a href=\"");
            writer.write(r.getName());
            writer.write("/\">");
            writer.write(r.getName());
            writer.write("</a></td><td>");
            File f = new File(configurationManager.getRoot(), r.getName());
            if (f.exists()) {
                writer.write(formatDate(f.lastModified()));
            }
            writer.write("</td><td></td></tr>");
        }
    }

}