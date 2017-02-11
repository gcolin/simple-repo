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
package net.gcolin.simplerepo.search;

import net.gcolin.simplerepo.IndexListener;
import net.gcolin.simplerepo.RepositoryListener;
import net.gcolin.simplerepo.maven.MavenInfo;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.model.ResultType;
import net.gcolin.simplerepo.model.SearchResult;
import net.gcolin.simplerepo.util.ConfigurationManager;

import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLEncoder;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Setup the search plugin.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class SearchListener implements ServletContextListener, RepositoryListener, IndexListener {

  public static final OutputStream DERBY_OUT = new DerbyToLogOutputStream();
  private SearchController controller;
  private ConfigurationManager configManager;

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.setProperty("derby.stream.error.field",
        "net.gcolin.simplerepo.search.SearchListener.DERBY_OUT");
    configManager = (ConfigurationManager) sce.getServletContext().getAttribute("configManager");
    try {
      controller = new SearchController(configManager);
      sce.getServletContext().setAttribute("searchcontroller", controller);
    } catch (IOException ex) {
      Logger.getLogger(SearchListener.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    controller.close();
    controller = null;
    sce.getServletContext().removeAttribute("searchcontroller");
    Enumeration<Driver> enumDriver = DriverManager.getDrivers();
    while(enumDriver.hasMoreElements()) {
      Driver driver = enumDriver.nextElement();
      if(driver.getClass().getName().contains("derby")){
        try {
          DriverManager.deregisterDriver(driver);
        } catch (SQLException ex) {
          logger.log(Level.SEVERE, null, ex);
        }
      }
    }
  }

  @Override
  public void onRecieveFile(File file, Repository repository, boolean remote, boolean override) {
    if(override) {
      return;
    }
    try {
      Model model = controller.detectModel(file);
      if(model != null) {
        if(file.getName().endsWith(".pom")) {
          controller.add(repository, file, model);
        } else {
          ResultType type = controller.getType(model, file.getName());
          if(type != null) {
            controller.add(model, type);
          }
        }
      }
    } catch (SQLException| IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void onRemoveFile(File file, Repository repository, boolean remote) {
    try {
      Model model = controller.detectModel(file);
      if(model != null) {
        if(file.getName().endsWith(".pom")) {
          controller.remove(model);
        }
        ResultType type = controller.getType(model, file.getName());
        if(type != null) {
          controller.remove(model, type);
        }
      }
    } catch (SQLException| IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
  }

  @Override
  public void onHead(HttpServletRequest req, Writer writer) throws IOException, ServletException {}

  @Override
  public void onStartBody(HttpServletRequest req, Writer writer)
      throws IOException, ServletException {}

  @Override
  public void onEndBody(HttpServletRequest req, Writer writer)
      throws IOException, ServletException {}

  @Override
  public void onIndex(HttpServletRequest req, Writer writer) throws IOException, ServletException {
    String query = req.getParameter("q");
    writer.write("<form><div class=\"input-group\">");
    writer.write(
        "<input type=\"text\" name=\"q\" class=\"form-control\" placeholder=\"Search for...\" value=\"");
    if (query != null) {
      writer.write(query);
    }
    writer.write("\">");
    writer.write("<span class=\"input-group-btn\">");
    writer.write(
        "<button class=\"btn btn-default\" type=\"submit\" rel=\"no-follow\"><span class=\"glyphicon glyphicon-search\"></span> Go!</button>");
    writer.write("</span></div></form>");

    String groupId = req.getParameter("groupId");
    String version = req.getParameter("version");
    if (version == null && (query != null || groupId != null)) {
      String artifactId = req.getParameter("artifactId");
      try {
        long count;
        if (query != null) {
          count = controller.count(query);
        } else if (artifactId != null) {
          count = controller.countByArtifactId(groupId, artifactId);
          writer.write("<ol class=\"breadcrumb\"><li><a href='");
          writer.write("?groupId=");
          writer.write(URLEncoder.encode(groupId, "utf-8"));
          writer.write("'>");
          writer.write(groupId);
          writer.write("</a></li><li class=\"active\">");
          writer.write(artifactId);
          writer.write("</li></ol>");
        } else {
          count = controller.countByGroupId(groupId);
          writer.write("<ol class=\"breadcrumb\"><li class=\"active\">");
          writer.write(groupId);
          writer.write("</li></ol>");
        }
        if (count > 0) {
          String first = req.getParameter("f");
          int offset = first == null ? 0 : Integer.parseInt(first);
          List<SearchResult> results;
          if (query != null) {
            results = controller.search(query, offset);
          } else if (artifactId != null) {
            results = controller.searchByArtifactId(groupId, artifactId, offset);
          } else {
            results = controller.searchByGroupId(groupId, offset);
          }
          if (!results.isEmpty()) {
            writer.write(
                "<table class=\"table\"><tr><th>groupId</th><th>artifactId</th><th>version</th></tr>");
            for (SearchResult result : results) {
              String encodedGroupId = URLEncoder.encode(result.getGroupId(), "utf-8");
              String encodedArtifactId = URLEncoder.encode(result.getArtifactId(), "utf-8");
              String encodedVersion = URLEncoder.encode(result.getVersion(), "utf-8");
              writer.write("<tr><td><a href='?groupId=");
              writer.write(encodedGroupId);
              writer.write("'>");
              writer.write(result.getGroupId());
              writer.write("</a></td><td><a href='?groupId=");
              writer.write(encodedGroupId);
              writer.write("&artifactId=");
              writer.write(encodedArtifactId);
              writer.write("'>");
              writer.write(result.getArtifactId());
              writer.write("</a></td><td><a href='?groupId=");
              writer.write(encodedGroupId);
              writer.write("&artifactId=");
              writer.write(encodedArtifactId);
              writer.write("&version=");
              writer.write(encodedVersion);
              writer.write("'>");
              writer.write(result.getVersion());
              writer.write("</a></td></tr>");
            }
            writer.write("</table>");
          }
          if (count > 20) {
            String basequery;
            if (query != null) {
              basequery = "?q=" + URLEncoder.encode(query, "utf-8") + "&f=";
            } else if (artifactId != null) {
              basequery = "?groupId=" + URLEncoder.encode(groupId, "utf-8") + "&artifactId="
                  + URLEncoder.encode(artifactId, "utf-8") + "&f=";
            } else {
              basequery = "?groupId=" + URLEncoder.encode(groupId, "utf-8") + "&f=";
            }
            writer.write("<nav aria-label=\"Page navigation\"><ul class=\"pagination\">");
            if (offset == 0) {
              writer.write(
                  "<li class=\"disabled\"><a href='javascript:void(0)'><span aria-hidden=\"true\">&laquo;</span></a></li>");
            } else {
              writer.write("<li><a href='");
              writer.write(basequery);
              writer.write(String.valueOf(Math.max(offset - 20, 0)));
              writer.write("'><span aria-hidden=\"true\">&laquo;</span></a></li>");
            }
            for (int i = 0; i < count; i += 20) {
              int page = i / 20 + 1;
              if (i == offset) {
                writer.write("<li class=\"disabled\"><a href='javascript:void(0)'>");
                writer.write(String.valueOf(page));
                writer.write("</a></li>");
              } else {
                writer.write("<li><a href='");
                writer.write(basequery);
                writer.write(String.valueOf(i));
                writer.write("'>");
                writer.write(String.valueOf(page));
                writer.write("</a></li>");
              }
            }
            if (count - offset > 20) {
              writer.write("<li><a href='");
              writer.write(basequery);
              writer.write(String.valueOf(offset + 20));
              writer.write("'><span aria-hidden=\"true\">&raquo;</span></a></li>");
            } else {
              writer.write(
                  "<li class=\"disabled\"><a href='javascript:void(0)'><span aria-hidden=\"true\">&raquo;</span></a></li>");
            }
          }
        } else {
          writer.write("<p class=\"text-info lead\">No result</p>");
        }
      } catch (SQLException ex) {
        throw new ServletException(ex);
      }
    } else if (version != null) {
      try {
        String artifactId = req.getParameter("artifactId");
        SearchResult result = controller.get(groupId, artifactId, version);
        if (result == null) {
          writer.write("<p class=\"text-info lead\">No result for ");
          writer.write(groupId);
          writer.write(":");
          writer.write(artifactId);
          writer.write(":");
          writer.write(version);
          writer.write("</p>");
        } else {
          MavenInfo.writeHtml(writer, result, configManager, controller, "repository/" + result.getRepoName() + "/", "", null);
        }
      } catch (SQLException ex) {
        throw new ServletException(ex);
      }
    }
  }
}
