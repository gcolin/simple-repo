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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Display index.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class AbstractDisplayServlet extends HttpServlet implements PluginListener {

  private static final long serialVersionUID = 1L;

  protected IndexListener[] listeners;

  /**
   * {@inheritDoc}
   */
  @Override

  public void init() {
    init0();
    PluginContainer container =
        (PluginContainer) getServletContext().getAttribute("pluginContainer");
    container.add(this);
  }

  protected abstract String getTitle();

  protected abstract void doContent(HttpServletRequest req, Writer writer)
      throws ServletException, IOException;

  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setCharacterEncoding("utf-8");
    resp.setContentType("text/html");
    Writer writer = resp.getWriter();
    writer.append("<html><head><title>");
    writer.append(getTitle());
    writer.append("</title>"
        + "<meta charset=\"UTF-8\"><meta name=\"viewport\" "
        + "content=\"width=device-width, initial-scale=1.0\">");

    req.setAttribute("title", getTitle());
    doMenu(req);

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onHead(req, writer);
    }

    writer.append("</head><body>");

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onStartBody(req, writer);
    }

    if (req.getAttribute("notitle") == null) {
      writer.append("<h1>");
      writer.append(getTitle());
      writer.append("</h1>");
    }

    doContent(req, writer);

    writeMenu(req, writer);

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onEndBody(req, writer);
    }

    writer.append("<footer class=\"footer\"><hr/><div class=\"container\"><p class=\"text-muted\">See the source at <a href='http://github.com/gcolin/simple-repo'>GitHub</a></p></div></footer></body></html>");
  }


  protected void doMenu(HttpServletRequest req) {
    String base = req.getContextPath();
    if (base.equals("/")) {
      base = "";
    }
    Map<String, String> menu = new LinkedHashMap<>();
    menu.put(base + "/repository/", "All repositories");
    menu.put(base + "/config/repository", "Repositories");
    PluginContainer container =
        (PluginContainer) getServletContext().getAttribute("pluginContainer");
    if (!container.getPlugins().isEmpty()) {
      menu.put(base + "/config/plugin", "Plugins");
    }
    menu.put(base + "/documentation", "Documentation");
    req.setAttribute("menu", menu);
  }

  @SuppressWarnings("unchecked")
  protected void writeMenu(HttpServletRequest req, Writer writer) throws IOException {
    for (Entry<String, String> entry : ((Map<String, String>) req.getAttribute("menu"))
        .entrySet()) {
      writer.append("<p><a href=\"");
      writer.append(entry.getKey());
      writer.append("\">");
      writer.append(entry.getValue());
      writer.append("</a></p>");
    }
  }

  @SuppressWarnings("unchecked")
  private void init0() {
    List<EventListener> pluginListeners =
        (List<EventListener>) getServletContext().getAttribute("pluginListeners");
    List<IndexListener> rlisteners = new ArrayList<IndexListener>();
    for (EventListener event : pluginListeners) {
      if (event instanceof IndexListener) {
        rlisteners.add((IndexListener) event);
      }
    }
    listeners = rlisteners.toArray(new IndexListener[rlisteners.size()]);
  }

  @Override
  public void onPluginInstalled() {
    init0();
  }

  @Override
  public void onPluginRemoved() {
    init0();
  }

  @Override
  public void onPluginUpdated(String name) {}

}
