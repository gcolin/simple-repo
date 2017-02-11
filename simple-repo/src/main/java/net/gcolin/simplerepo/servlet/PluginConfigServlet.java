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

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.gcolin.simplerepo.PluginContainer;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;

/**
 * A servlet for configuring the plugins.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class PluginConfigServlet extends AbstractDisplayServlet {

  private static final long serialVersionUID = -1900367253812077237L;

  private transient ExecutorService executor;

  @Override
  protected String getTitle() {
    return "Simple repo - Plugins";
  }

  @Override
  public void init() {
    executor = Executors.newCachedThreadPool();
    super.init();
  }

  @Override
  public void destroy() {
    executor.shutdownNow();
    executor = null;
    super.destroy();
  }
  
  @Override
  protected void doContent(HttpServletRequest req, Writer writer)
      throws ServletException, IOException {
    PluginContainer container =
        (PluginContainer) req.getServletContext().getAttribute("pluginContainer");

    ConfigurationManager configManager =
        (ConfigurationManager) req.getServletContext().getAttribute("configManager");

    String msg = configManager.getCurrentAction();
    if (msg != null) {
      writer.append("<div class=\"alert alert-info\" role=\"alert\">");
      writer.append(msg);
      writer.append("</div>");
    }

    for (Properties props : container.getPlugins().values()) {
      String name = props.getProperty("name");
      writer.append("<div class=\"panel panel-default\"><div class=\"panel-heading\">");
      writer.append(name);
      writer.append("</div><div class=\"panel-body\">");
      if (props.containsKey("description")) {
        writer.append("<p>");
        writer.append(props.getProperty("description"));
        writer.append("</p>");
      }

      if (container.getActivePlugins().contains(name)) {
        Map<String, String> actions = new TreeMap<>();
        Map<String, String> config = new TreeMap<>();
        for (Object key : props.keySet()) {
          if (key.toString().startsWith("config.")) {
            config.put(key.toString(), props.getProperty(key.toString()));
          } else if (key.toString().startsWith("action.")) {
            actions.put(key.toString(), props.getProperty(key.toString()));
          }
        }

        if (!config.isEmpty()) {
          writer.append("<div class='well'><form method='POST'>");
          writer.append("<input name='name' type='hidden' value='");
          writer.append(name);
          writer.append("'/>");
          writer.append("<input name='config' type='hidden' value='true'/>");
          for (Entry<String, String> cf : config.entrySet()) {
            try {
              String actualValue = configManager.getProperty(cf.getKey());
              if (actualValue == null) {
                actualValue = "";
              }
              writer.append("<div class=\"form-group\"><label for=\"");
              writer.append(cf.getKey());
              writer.append("\">");
              String description = props.getProperty("description." + cf.getKey());
              if (description == null) {
                description = cf.getKey().substring(7);
              }
              writer.append(description);
              writer.append("</label>");
              Class<?> type = this.getClass().getClassLoader().loadClass(cf.getValue());
              if (type.isEnum()) {
                writer.append("<select class=\"form-control\" id=\"");
                writer.append(cf.getKey());
                writer.append("\" name=\"");
                writer.append(cf.getKey());
                writer.append("\">");
                for (Object value : type.getEnumConstants()) {
                  String val = value.toString();
                  writer.append("<option value=\"");
                  writer.append(val);
                  writer.append("\"");
                  if (val.equals(actualValue)) {
                    writer.append("selected=\"selected\"");
                  }
                  writer.append(">");
                  writer.append(val);
                  writer.append("</option>");
                }
                writer.append("</select>");
              } else if (type == Boolean.class) {
                writer.append("<input type=\"checkbox\" class=\"checkbox\" id=\"");
                writer.append(cf.getKey());
                writer.append("\" name=\"");
                writer.append(cf.getKey());
                writer.append("\"");
                if(Boolean.parseBoolean(actualValue) || "on".equals(actualValue)) {
                  writer.append(" checked=\"checked\"");
                }
                writer.append("/>");
              } else if (type == Repository.class) {
                writer.append("<select class=\"form-control\" id=\"");
                writer.append(cf.getKey());
                writer.append("\" name=\"");
                writer.append(cf.getKey());
                writer.append("\">");
                for (String val : configManager.getRepos().keySet()) {
                  writer.append("<option value=\"");
                  writer.append(val);
                  writer.append("\"");
                  if (val.equals(actualValue)) {
                    writer.append("selected=\"selected\"");
                  }
                  writer.append(">");
                  writer.append(val);
                  writer.append("</option>");
                }
                writer.append("</select>");
              } else {
                writer.append("<input type=\"text\" class=\"form-control\" id=\"");
                writer.append(cf.getKey());
                writer.append("\" name=\"");
                writer.append(cf.getKey());
                writer.append("\" value=\"");
                writer.append(actualValue);
                writer.append("\"/>");
              }
              writer.append("</div>");
            } catch (ClassNotFoundException ex) {
              throw new ServletException(ex);
            }
          }
          writer.append("<button class='btn btn-primary' type='submit'>Save</button>");
          writer.append("</form></div>");
        }

        if (!actions.isEmpty()) {
          for (Entry<String, String> cf : actions.entrySet()) {
            writer.append("<form method='POST' class='form-inline'>");
            writer.append("<input name='name' type='hidden' value='");
            writer.append(name);
            writer.append("'/>");
            writer.append("<input name='action' type='hidden' value='");
            writer.append(cf.getValue());
            writer.append("'/><button class='btn btn-default'>");
            String description = props.getProperty("description." + cf.getKey());
            if (description == null) {
              description = cf.getKey().substring(7);
            }
            writer.append(description);
            writer.append("</button></form>");
          }
        }

      }

      writer.append("<form method='POST'>");
      writer.append("<input name='name' type='hidden' value='");
      writer.append(name);
      writer.append("'/>");
      if (container.getActivePlugins().contains(name)) {
        writer.append("<input name='active' type='hidden' value='false'/>");
        writer.append("<button class='btn btn-warning' type='submit'>Disable</button></form>");
      } else {
        writer.append("<input name='active' type='hidden' value='true'/>");
        writer.append("<button class='btn btn-default' type='submit'>Enable</button></form>");
      }
      writer.append("</div></div>");
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String name = req.getParameter("name");
    String active = req.getParameter("active");
    String action = req.getParameter("action");

    PluginContainer container =
        (PluginContainer) req.getServletContext().getAttribute("pluginContainer");

    if (active != null) {
      if (Boolean.parseBoolean(active)) {
        container.installPlugin(container.getPlugins().get(name));
      } else {
        container.removePlugin(container.getPlugins().get(name));
      }
    } else if (action != null) {
      final String[] parts = action.split("\\.");
      if (parts.length < 2) {
        throw new ServletException("bad action");
      }
      final Object obj = getServletContext().getAttribute(parts[0].trim());
      if (parts.length > 2 && parts[2].trim().equals("async")) {
        executor.submit(new Runnable() {

          @Override
          public void run() {
            try {
              obj.getClass().getMethod(parts[1].trim()).invoke(obj);
            } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException ex) {
              Logger.getLogger(PluginConfigServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
          }
        });
      } else {
        try {
          obj.getClass().getMethod(parts[1].trim()).invoke(obj);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
          throw new ServletException(ex);
        }
      }
    } else {
      ConfigurationManager configManager =
          (ConfigurationManager) req.getServletContext().getAttribute("configManager");

      Properties props = container.getPlugins().get(name);
      for (Object key : props.keySet()) {
        if (key.toString().startsWith("config.")) {
          configManager.setProperty(key.toString(), req.getParameter(key.toString()));
        }
      }

      container.onPluginUpdated(name);
    }

    doGet(req, resp);
  }

}
