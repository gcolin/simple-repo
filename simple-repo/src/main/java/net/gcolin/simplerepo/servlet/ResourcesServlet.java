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
package net.gcolin.simplerepo.servlet;

import net.gcolin.server.maven.PluginContainer;
import net.gcolin.server.maven.PluginListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Admin
 */
public class ResourcesServlet extends AbstractIoServlet implements PluginListener {

  private static final long serialVersionUID = 1L;
  private transient Map<String, Item> resources = new ConcurrentHashMap<String, Item>();
  private transient Collection<ClassLoader> pluginsClassLoaders;

  @Override
  public void init() {
    PluginContainer container =
        (PluginContainer) getServletContext().getAttribute("pluginContainer");
    container.add(this);
    init0();
  }

  @SuppressWarnings("unchecked")
  private void init0() {
    pluginsClassLoaders =
        (Collection<ClassLoader>) getServletContext().getAttribute("pluginsClassLoaders");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String path = req.getPathInfo();
    if (path == null || path.length() == 0) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    Item item = resources.get(path);
    if (item == null) {
      URL url = null;
      String rpath = "META-INF/resources" + path;
      ClassLoader cl = null;
      for (ClassLoader pluginsClassLoader : pluginsClassLoaders) {
        url = pluginsClassLoader.getResource(rpath);
        if (url != null) {
          cl = pluginsClassLoader;
          break;
        }
      }
      item = new Item();
      item.classLoader = cl;
      item.url = url;
      if (url != null) {
        String file = url.getFile();
        if (file != null) {
          if (file.startsWith("file:/")) {
            file = file.substring(6);
          }
          int split = file.indexOf("!/");
          if (split != -1) {
            file = file.substring(0, split);
            File jar = new File(file);
            if (jar.exists()) {
              item.lastUpdate = jar.lastModified() / 1000 * 1000;
            }
          }
        }
      }
      resources.put(path, item);
    }
    if (item.url == null) {
      resp.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    Enumeration<String> en = req.getHeaders("If-Modified-Since");
    if (en.hasMoreElements()) {
      long date = req.getDateHeader("If-Modified-Since");
      if (item.lastUpdate <= date) {
        resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return;
      }
    }
    if (item.length == -1) {
      InputStream in = null;
      try {
        in = item.url.openStream();
        long count;
        long sum = 0;
        while ((count = in.skip(1024L)) > 0) {
          sum += count;
        }
        item.length = (int) sum;
      } finally {
        close(in);
      }
    }
    resp.setContentLength(item.length);
    resp.setDateHeader("Last-Modified", item.lastUpdate);
    InputStream in = null;
    try {
      in = item.url.openStream();
      copy(in, resp.getOutputStream());
    } finally {
      close(in);
    }
  }

  private static class Item {

    private long lastUpdate = System.currentTimeMillis() / 1000 * 1000;
    private URL url;
    private int length = -1;
    private ClassLoader classLoader;
  }

  @Override
  public synchronized void onPluginInstalled(ClassLoader classLoader) {
    init0();
  }

  @Override
  public synchronized void onPluginRemoved(ClassLoader classLoader) {
    init0();
    Set<String> toRemove = new HashSet<>();
    for (Entry<String, Item> entry : resources.entrySet()) {
      if (entry.getValue().classLoader == classLoader) {
        toRemove.add(entry.getKey());
      }
    }
    for (String key : toRemove) {
      resources.remove(key);
    }
  }
}
