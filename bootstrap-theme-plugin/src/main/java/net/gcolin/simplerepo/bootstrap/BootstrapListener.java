/*
 * Copyright 2017 Admin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.bootstrap;

import net.gcolin.simplerepo.IndexListener;
import net.gcolin.simplerepo.PluginListener;
import net.gcolin.simplerepo.util.ConfigurationManager;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;

/**
 * Display bootstrap theme.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class BootstrapListener implements ServletContextListener, IndexListener, PluginListener {

  private String theme;
  private ServletContext context;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    context = sce.getServletContext();
    init0();
  }

  private void init0() {
    ConfigurationManager configManager =
        (ConfigurationManager) context.getAttribute("configManager");
    theme = configManager.getProperty("config.theme");
    if (theme == null) {
      theme = "default";
    } else {
      theme = theme.trim().toLowerCase();
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {}

  @Override
  public void onHead(HttpServletRequest req, Writer writer) throws IOException {
    String base = getBase(req);
    writer.append("<link href=\"");
    writer.append(base);
    writer.append("/resources/bootstrap/css/");

    String btheme = (String) req.getAttribute("theme");
    if (btheme == null) {
      btheme = theme;
    } else {
      btheme = btheme.trim().toLowerCase();
    }
    writer.append(btheme);
    writer.append(".css\" rel=\"stylesheet\">");
    writer.append("<link href=\"");
    writer.append(base);
    writer.append("/resources/bootstrap/css/sticky-footer.css\" rel=\"stylesheet\">");
    writer.append("<script type='text/javascript' src=\"");
    writer.append(base);
    writer.append("/resources/jquery/jquery-3.1.0.min.js\"></script>");
    writer.append("<script type='text/javascript' src=\"");
    writer.append(base);
    writer.append("/resources/bootstrap/js/bootstrap.min.js\"></script>");
    req.setAttribute("nomenu", true);
    req.setAttribute("notitle", true);
  }

  private String getBase(HttpServletRequest req) {
    String base = (String) req.getAttribute("base");
    if (base == null) {
      base = req.getContextPath();
    }
    if ("/".equals(base)) {
      base = "";
    }
    return base;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void onStartBody(HttpServletRequest req, Writer writer) throws IOException {
    writer.append("<nav class=\"navbar navbar-inverse\">");
    writer.append("<div class=\"container\">");
    writer.append("<div class=\"navbar-header\">");
    writer.append(
        "<button type=\"button\" class=\"navbar-toggle collapsed\" data-toggle=\"collapse\" data-target=\"#navbar\" aria-expanded=\"false\" aria-controls=\"navbar\">");
    writer.append("<span class=\"sr-only\">Toggle navigation</span>");
    writer.append("<span class=\"icon-bar\"></span>");
    writer.append("<span class=\"icon-bar\"></span>");
    writer.append("<span class=\"icon-bar\"></span>");
    writer.append("</button><a class=\"navbar-brand\" href=\"");
    writer.append(getBase(req));
    writer.append("/\">Simple repo</a></div>");
    writer.append("<div id=\"navbar\" class=\"collapse navbar-collapse\">");
    writer.append("<ul class=\"nav navbar-nav\">");
    Map<String, String> menu = (Map<String, String>) req.getAttribute("menu");
    for (Entry<String, String> entry : menu.entrySet()) {
      writer.append("<li");
      if (entry.getKey().equals(req.getRequestURI())) {
        writer.append(" class=\"active\"");
      }
      writer.append("><a href=\"");
      writer.append(entry.getKey());
      writer.append("\">");
      writer.append(entry.getValue());
      writer.append("</a></li>");
    }
    menu.clear();
    writer.append("</ul></div></div></nav>");
    writer.append("<div class=\"container\">");
  }

  @Override
  public void onEndBody(HttpServletRequest req, Writer writer) throws IOException {
    writer.append("</div>");
  }

  @Override
  public void onIndex(HttpServletRequest req, Writer writer) throws IOException {}

  @Override
  public void onPluginInstalled() {}

  @Override
  public void onPluginRemoved() {}

  @Override
  public void onPluginUpdated(String name) {
    if ("Bootstrap".equals(name)) {
      init0();
    }
  }

}
