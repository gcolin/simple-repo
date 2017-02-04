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
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.gcolin.server.maven.IndexListener;
import net.gcolin.server.maven.PluginContainer;
import net.gcolin.server.maven.PluginListener;

/**
 * Display index.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class IndexServlet extends HttpServlet implements PluginListener {

  private static final long serialVersionUID = 1L;

  private IndexListener[] listeners;

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
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setCharacterEncoding("utf-8");
    resp.setContentType("text/html");
    Writer writer = resp.getWriter();
    writer.append("<html><head><title>Simple repo</title>"
        + "<meta charset=\"UTF-8\"><meta name=\"viewport\" "
        + "content=\"width=device-width, initial-scale=1.0\">");

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onHead(req, writer);
    }

    writer.append("</head><body>");

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onStartBody(req, writer);
    }

    if (req.getAttribute("notitle") == null) {
      writer.append("<h1>Simple repo</h1>");
    }

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onIndex(req, writer);
    }

    if (req.getAttribute("nomenu") == null) {
      writer.append("<p><a href=\"repository/\">All repositories</a></p>"
          + "<p><a href=\"docs.html\">Documentation</a></p>");
    }

    for (int i = 0; i < listeners.length; i++) {
      listeners[i].onEndBody(req, writer);
    }

    writer.append("</body></html>");
  }

  @Override
  public void onPluginInstalled(ClassLoader classLoader) {
    init0();
  }

  @Override
  public void onPluginRemoved(ClassLoader classLoader) {
    init0();
  }

}
