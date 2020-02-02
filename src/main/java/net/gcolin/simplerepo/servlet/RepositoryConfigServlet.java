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

import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.ConfigurationManager.RepoHandle;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet for configuring the repositories.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepositoryConfigServlet extends AbstractDisplayServlet {

  private static final long serialVersionUID = 8125626220419384344L;

  @Override
  protected void doContent(HttpServletRequest req, Writer writer)
      throws ServletException, IOException {
    writer.write(
        "<div class='row'><div class='col c3'><h3>Repositories</h3><ul>");
    ConfigurationManager configManager =
        (ConfigurationManager) getServletContext().getAttribute("configManager");
    String reponame = (String) req.getAttribute("r");
    if (reponame == null) {
      reponame = req.getParameter("r");
    }
    Repository selected = null;
    for (Repository repo : configManager.getRepos().values()) {
      writer.write("<li role='presentation'");
      if (repo.getName().equals(reponame)) {
        writer.write(" class='active'");
        selected = repo;
      }
      writer.write("><a href='?r=");
      writer.write(URLEncoder.encode(repo.getName(), "utf-8"));
      writer.write("'>");
      writer.write(repo.getName());
      writer.write("</a></li>");
    }
    writer.write("</ul>");
    writer.write(
            "<div><a class='btn btn-a btn-sm' href='?a=new'>New repository</a> <br/>");
        writer.write(
            "<a class='btn btn-sm' href='?a=global'>Global configuration</a></div><br/>");
    writer.write("</div><div class='col c9'>");
    boolean isNew = reponame == null && "new".equals(req.getParameter("a"));
    if (isNew) {
      selected = new Repository();
    }
    

    if ("global".equals(req.getParameter("a"))) {
      writer.write(
          "<div><h2>Global configuration</h2>");
      writeMessages(req, writer);
      writer.write("<form method=\"POST\"><input type=\"hidden\" name=\"a\" value='global'/><div class=\"row\">");
      writer.write("<div class=\"col c3\"><label for=\"maxsnapshots\">Max snapshots</label></div>");
      writer.write(
          "<div class=\"col c9\"><input type=\"text\" name=\"maxsnapshots\" id=\"maxsnapshots\" placeholder=\"The max number of snaphots by artifact\" value='");
      if (req.getParameter("maxsnapshots") != null) {
        writer.write(req.getParameter("maxsnapshots"));
      } else {
        writer.write(String.valueOf(configManager.getMaxSnapshots()));
      }
      writer.write("'/></div>");
      writer.write("<div class=\"col c3\"><label for=\"maxsnapshots\">Not Found Cache</label></div>");
      writer.write(
          "<div class=\"col c9\"><input type=\"text\" name=\"notfoundcache\" id=\"notfoundcache\" placeholder=\"The time in millisenconds before retrying to get a not found file\" value='");
      if (req.getParameter("notfoundcache") != null) {
        writer.write(req.getParameter("notfoundcache"));
      } else {
        writer.write(String.valueOf(configManager.getNotFoundCache()));
      }
      writer.write("'/></div>");
      writer.write(
          "<button type=\"submit\" class=\"btn btn-b btn-sm\">Save</button>");
      writer.write("</form></div></div>");
    } else if (selected != null) {
      writer.write(
          "<div><h2>");
      writer.write(isNew ? "New" : "Edit");
      writer.write(
          " repository </h2>");
      writeMessages(req, writer);
      writer.write("<form id='form' method=\"POST\"><div class=\"row\">");
      writer.write("<div class=\"form-group\">");
      if (!isNew) {
        writer.write("<input type=\"hidden\" name=\"r\" value='");
        if (selected.getName() != null) {
          writer.write(selected.getName());
        }
        writer.write("'/>");
        writer.write("<input type=\"hidden\" id='action' name=\"a\" value=''/>");
      } else {
        writer.write("<input type=\"hidden\" name=\"a\" value='new'/>");
      }
      writer.write("<div class=\"col c3\"><label for=\"name\">Name</label></div>");
      writer.write(
          "<div class=\"col c9\"><input type=\"text\" name=\"name\" id=\"name\" placeholder=\"Name\" value='");
      if (req.getParameter("name") != null) {
        writer.write(req.getParameter("name"));
      } else if (selected.getName() != null) {
        writer.write(selected.getName());
      }
      writer.write("'/></div>");
      writer.write("<div class=\"col c3\"><label for=\"remote\">Remote URL</label></div>");
      writer.write(
          "<div class=\"col c9\"><input type=\"text\" name=\"remote\" id=\"remote\" placeholder=\"URL\" value='");
      if (req.getParameter("remote") != null) {
        writer.write(req.getParameter("remote"));
      } else if (selected.getRemote() != null) {
        writer.write(selected.getRemote());
      }
      writer.write("'/></div>");
      writer.write("<div class=\"col c3\">");
      writer.write("<label for=\"included\">Includes</label></div>");
      writer
          .write("<div class=\"col c9\"><select id=\"included\" name=\"included\" multiple=\"\" class=\"form-control\">");
      for (Repository repo : configManager.getRepos().values()) {
        if (repo != selected) {
          writer.write("<option value='");
          writer.write(repo.getName());
          writer.write("'");
          if (selected.getIncludes() != null && selected.getIncludes().contains(repo.getName())) {
            writer.write(" selected=\"selected\"");
          }
          writer.write(">");
          writer.write(repo.getName());
          writer.write("</option>");
        }
      }
      writer.write("</select></div>");
      writer.write(
          "<button type=\"submit\" class=\"btn btn-b btn-sm\">");
      writer.write(isNew ? "Create" : "Update");
      writer.write("</button>");
      if (!isNew) {
        writer.write(
            " <button type=\"button\" onclick=\"javascript:document.getElementById('action').value='del';document.getElementById('form').submit()\" class=\"btn btn-c btn-sm\">Remove</button>");
      }
      writer.write("</form></div></div>");
    }
    writer.write("</div></div>");
  }

  private void writeMessages(HttpServletRequest req, Writer writer) throws IOException {
    if (req.getAttribute("message") != null) {
      writer.write(
          "<div class=\"msg\">");
      writer.write((String) req.getAttribute("message"));
      writer.write("</div>");
    }
  }

  @Override
  protected String getTitle() {
    return "Simple repo - Repositories";
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String[] actions = req.getParameterValues("a");
    
    String action;
    if(actions == null) {
      action = null;
    } else if(actions.length == 1) {
      action = actions[0];
    } else {
      List<String> list = Arrays.asList(actions);
      if(list.contains("del")) {
        action = "del";
      } else {
        action = list.get(0);
      }
    }

    ConfigurationManager configManager =
        (ConfigurationManager) getServletContext().getAttribute("configManager");

    if ("global".equals(action)) {
      try {
        int maxsnapshots = Integer.parseInt(req.getParameter("maxsnapshots"));
        long notfoundcache = Long.parseLong(req.getParameter("notfoundcache"));
        if (maxsnapshots != configManager.getMaxSnapshots()) {
          configManager.setMaxSnapshots(maxsnapshots);
        }
        if (notfoundcache != configManager.getNotFoundCache()) {
          configManager.setNotFoundCache(maxsnapshots);
        }
      } catch (NumberFormatException ex) {
        req.setAttribute("message", "Bad number format " + ex.getMessage());
        doGet(req, resp);
        return;
      }
      req.setAttribute("message", "Saved");
      doGet(req, resp);
      return;
    }

    String reponame = req.getParameter("r");
    String name = req.getParameter("name");
    Repository repo = null;

    if (reponame == null && (name == null || name.isEmpty())) {
      req.setAttribute("message", "The new repository must have a name");
      doGet(req, resp);
      return;
    }

    if (reponame != null) {
      repo = configManager.getRepository(reponame);
      if (repo == null) {
        req.setAttribute("message", "The repository does not exists");
        doGet(req, resp);
        return;
      }
    }

    String[] includes = req.getParameterValues("included");
    if (includes == null) {
      includes = new String[0];
    }
    String remote = req.getParameter("remote");

    if (remote != null && !remote.isEmpty() && includes != null && includes.length > 0) {
      req.setAttribute("message",
          "The repository cannot have a remote URL and include other repositories");
      doGet(req, resp);
      return;
    }

    RepoHandle handle = configManager.toHandle(repo);
    try {
      if ("del".equals(action)) {
        handle.remove();
        doGet(req, resp);
        return;
      }

      if (reponame == null) {
        configManager.newRepository(name);
        repo = configManager.getRepository(name);
        handle = configManager.toHandle(repo);
      } else if (!Objects.equals(repo.getName(), name)) {
        handle.rename(name);
      }
      req.setAttribute("r", repo.getName());
      if (!Objects.equals(repo.getRemote(), remote)) {
        handle.updateRemoteUrl(remote);
      }
      List<String> repoIncludes = new ArrayList<>();
      if (repo.getIncludes() != null) {
        repoIncludes.addAll(repo.getIncludes());
      }
      for (String include : includes) {
        if (repoIncludes.contains(include)) {
          repoIncludes.remove(include);
        } else {
          handle.addInclude(include);
        }
      }
      for (String include : repoIncludes) {
        handle.removeInclude(include);
      }
      req.setAttribute("message", "Saved");
      doGet(req, resp);
    } catch (IllegalArgumentException ex) {
      req.setAttribute("message", ex.getMessage());
      doGet(req, resp);
      return;
    }

  }

}
