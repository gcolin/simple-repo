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
package net.gcolin.simplerepo.model;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import net.gcolin.simplerepo.business.ConfigurationManager;
import net.gcolin.simplerepo.servlet.Util;

/**
 * FreeMarkerModel model.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class FreeMarkerModel {

  private String path;

  private String currentPath;

  private ConfigurationManager configurationManager;

  private HttpServletRequest request;

  public FreeMarkerModel(HttpServletRequest request, ConfigurationManager configurationManager) {
    this.request = request;
    if (request != null) {
      path = request.getContextPath();
      if ("/".equals(path)) {
        path = "";
      }
      currentPath = request.getRequestURI();
    }
    this.configurationManager = configurationManager;
  }

  public Object attr(String name) {
    return request.getAttribute(name);
  }

  public String getPath() {
    return path;
  }

  public String getCurrentPath() {
    return currentPath;
  }

  public Configuration getConfiguration() {
    return configurationManager.getConfiguration();
  }

  public ConfigurationManager getConfigurationManager() {
    return configurationManager;
  }

  public String encode(String value) {
    return Util.encode(value);
  }

  public String max(int v1, int v2) {
    return String.valueOf(Math.max(v1, v2));
  }

  public boolean toBool(Boolean b) {
    return b;
  }

  public String[] getThemes() {
    return new String[] {"cerulean", "cosmo", "cyborg", "darkly", "default", "flatly", "journal",
        "lumen", "paper", "readable", "sandstone", "simplex", "slate", "spacelab", "superhero",
        "united", "yeti"};
  }
}
