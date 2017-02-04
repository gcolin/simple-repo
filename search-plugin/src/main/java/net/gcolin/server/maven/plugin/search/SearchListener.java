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
package net.gcolin.server.maven.plugin.search;

import net.gcolin.server.maven.IndexListener;
import net.gcolin.server.maven.RepositoryListener;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.JmxUtil;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Admin
 */
public class SearchListener implements ServletContextListener, RepositoryListener, IndexListener {

  public static final OutputStream DERBY_OUT = new DerbyToLogOutputStream();
  private SearchController controller;
  private ConfigurationManager configManager;
  private static final String[] SCOPES =
      new String[] {"compile", "runtime", "test", "system", "provided"};

  private static final String[] SCOPES_TITLE =
      new String[] {"Compile", "Runtime", "Test", "System", "Provided"};

  private String getControllerJmxName(ConfigurationManager configManager) {
    if (configManager.getContextName() == null) {
      return "net.gcolin.simplerepo.plugin.search:type=Controller";
    } else {
      return "net.gcolin.simplerepo.plugin.search:ctx=" + configManager.getContextName()
          + ",type=Controller";
    }
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    System.setProperty("derby.stream.error.field",
        "net.gcolin.server.maven.plugin.search.SearchListener.DERBY_OUT");
    configManager = (ConfigurationManager) sce.getServletContext().getAttribute("configManager");
    try {
      controller = new SearchController(configManager);
    } catch (IOException ex) {
      Logger.getLogger(SearchListener.class.getName()).log(Level.SEVERE, null, ex);
    }
    JmxUtil.publish(getControllerJmxName(configManager), controller, SearchControllerJmx.class);

  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    JmxUtil.unpublish(getControllerJmxName(configManager));
  }

  @Override
  public void onRecieveFile(File file, Repository repository, boolean remote, boolean override) {}

  @Override
  public void onRemoveFile(File file, Repository repository, boolean remote) {}

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
              int page = (i / 20) + 1;
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
          writer.write("<ol class=\"breadcrumb\"><li><a href='");
          writer.write("?groupId=");
          writer.write(URLEncoder.encode(groupId, "utf-8"));
          writer.write("'>");
          writer.write(groupId);
          writer.write("</a></li><li><a href='");
          writer.write("?groupId=");
          writer.write(URLEncoder.encode(groupId, "utf-8"));
          writer.write("&artifactId=");
          writer.write(URLEncoder.encode(artifactId, "utf-8"));
          writer.write("'>");
          writer.write(artifactId);
          writer.write("</a></li><li class=\"active\">");
          writer.write(version);
          writer.write("</li></ol>");
          File pom = new File(configManager.getRoot(),
              result.getRepoName() + "/" + groupId.replace('.', '/') + "/" + artifactId + "/"
                  + version + "/" + artifactId + "-" + version + ".pom");
          Model model;
          try (InputStream in = new FileInputStream(pom)) {
            model = new MavenXpp3Reader().read(in);
            model.setPomFile(pom);
          }
          
          if(model.getVersion() == null && model.getParent() != null) {
            model.setVersion(model.getParent().getVersion());
          }
          
          if(model.getGroupId() == null && model.getParent() != null) {
            model.setGroupId(model.getParent().getGroupId());
          }
          
          Properties props = new Properties();
          fillProperties(model, req, result.getRepoName(), props);
                    
          writer.write("<h1>");
          writer.write(Resolver.resolve(model.getName(), props, model));
          writer.write("</h1>");
          List<License> licenses = getLicenses(model, req, result.getRepoName());
          if (!licenses.isEmpty()) {
            writer.write("<div class='pull-right'>");
            for (License license : licenses) {
              if (license.getUrl() != null) {
                writer.write(" <a target='_blank' href='");
                writer.write(license.getUrl());
                writer.write("'>");
              }
              writer.write("<span class=\"label label-info\">");
              writer.write(license.getName());
              writer.write("</span>");
              if (license.getUrl() != null) {
                writer.write("</a>");
              }
            }

            writer.write("</div>");
          }
          writer.write("<p class='text-muted'><i>");
          writer.write(new SimpleDateFormat("MMM dd, yyyy").format(pom.lastModified()));
          writer.write("</i></p>");
          if (model.getUrl() != null) {
            writer.write("<p>");
            writer.write(" <a target='_blank' href='");
            writer.write(model.getUrl());
            writer.write("'>");
            writer.write(model.getUrl());
            writer.write("</a></p>");
          }

          writer.write("<p>");
          if (model.getDescription() != null) {
            writer.write(model.getDescription());
          }
          writer.write("</p>");

          writer.write("<ul class=\"nav nav-tabs\" id=\"code\" role=\"tablist\">");
          writer.write(
              "<li role=\"presentation\" class=\"active\"><a href=\"#maven\" id=\"maven-tab\" role=\"tab\" data-toggle=\"tab\" aria-controls=\"maven\" aria-expanded=\"true\">Maven</a></li>");
          for (String name : new String[] {"Gradle", "SBT", "Ivy", "Grape", "Leiningen",
              "Buildr"}) {
            writer.write(MessageFormat.format(
                "<li role=\"presentation\"><a href=\"#{1}\" role=\"tab\" id=\"{1}-tab\" data-toggle=\"tab\" aria-controls=\"{1}\">{0}</a></li>",
                name, name.toLowerCase()));
          }
          writer.write(
              "</ul><div class=\"tab-content\" id=\"codeContent\"><div class=\"tab-pane fade in active\" role=\"tabpanel\" id=\"maven\" aria-labelledby=\"maven-tab\"><pre>");
          writer.write("\n&lt;dependency&gt;");
          writer.write("\n    &lt;groupId&gt;");
          writer.write(groupId);
          writer.write("&lt;/groupId&gt;");
          writer.write("\n    &lt;artifactId&gt;");
          writer.write(artifactId);
          writer.write("&lt;/artifactId&gt;");
          writer.write("\n    &lt;version&gt;");
          writer.write(version);
          writer.write("&lt;/version&gt;");
          writer.write("\n&lt;/dependency&gt;</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"gradle\" aria-labelledby=\"gradle-tab\"><pre>");
          writer.write("\ncompile group: '");
          writer.write(groupId);
          writer.write("', name: '");
          writer.write(artifactId);
          writer.write("', version: '");
          writer.write(version);
          writer.write("'</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"sbt\" aria-labelledby=\"sbt-tab\"><pre>");
          writer.write("\nlibraryDependencies += \"");
          writer.write(groupId);
          writer.write("\" % \"");
          writer.write(artifactId);
          writer.write("\" % \"");
          writer.write(version);
          writer.write("\"</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"ivy\" aria-labelledby=\"ivy-tab\"><pre>");
          writer.write("\n&lt;dependency org=\"");
          writer.write(groupId);
          writer.write("\" name=\"");
          writer.write(artifactId);
          writer.write("\" rev=\"");
          writer.write(version);
          writer.write("\"/&gt;</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"grape\" aria-labelledby=\"grape-tab\"><pre>");
          writer.write("\n@Grapes(");
          writer.write("\n    @Grab(group='");
          writer.write(groupId);
          writer.write("', module='");
          writer.write(artifactId);
          writer.write("', version='");
          writer.write(version);
          writer.write("')\n)</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"leiningen\" aria-labelledby=\"leiningen-tab\"><pre>");
          writer.write("\n[");
          writer.write(groupId);
          writer.write("/");
          writer.write(artifactId);
          writer.write(" \"");
          writer.write(version);
          writer.write("\"]</pre></div>");
          writer.write(
              "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"buildr\" aria-labelledby=\"buildr-tab\"><pre>");
          writer.write("\n'");
          writer.write(groupId);
          writer.write(":");
          writer.write(artifactId);
          writer.write(":");
          writer.write(model.getPackaging());
          writer.write(":");
          writer.write(version);
          writer.write("'</pre></div></div>");

          Collections.sort(result.getTypes(), new Comparator<ResultType>() {

            @Override
            public int compare(ResultType o1, ResultType o2) {
              int type = o1.getName().compareTo(o2.getName());
              if (type != 0) {
                return type;
              }
              return o1.getClassifier().compareTo(o2.getClassifier());
            }

          });

          writer.write("<h3>Files</h3><ul>");
          for (ResultType resultType : result.getTypes()) {
            writer.write("<li><a target='_blank' href='repository/");
            writer.write(result.getRepoName());
            writer.write("/");
            writer.write(result.getGroupId().replace('.', '/'));
            writer.write("/");
            writer.write(result.getArtifactId());
            writer.write("/");
            writer.write(result.getVersion());
            writer.write("/");
            writer.write(result.getArtifactId());
            writer.write("-");
            writer.write(result.getVersion());
            if (!resultType.getClassifier().isEmpty()) {
              writer.write("-");
              writer.write(resultType.getClassifier());
            }
            writer.write(".");
            writer.write(resultType.getName());
            writer.write("'>");
            if (!resultType.getClassifier().isEmpty()) {
              writer.write(resultType.getClassifier());
              writer.write(".");
            }
            writer.write(resultType.getName());
            writer.write("</a></li>");
          }
          writer.write("</ul>");
          List<Dependency> dependencies = getDependencies(model, req, result.getRepoName());
          for (int i = 0; i < SCOPES.length; i++) {
            String scope = SCOPES[i];
            List<Dependency> dscope = new ArrayList<>();
            for (Dependency ds : dependencies) {
              if (scope.equals(ds.getScope())) {
                dscope.add(ds);
              }
            }
            if (!dscope.isEmpty() || i == 0) {
              writer.write("<h3>");
              writer.write(SCOPES_TITLE[i]);
              writer.write(" dependencies (");
              writer.write(String.valueOf(dscope.size()));
              writer.write(")</h3><ul>");
              for (Dependency ds : dscope) {
                ds.setGroupId(Resolver.resolve(ds.getGroupId(), props, model));
                ds.setArtifactId(Resolver.resolve(ds.getArtifactId(), props, model));
                ds.setVersion(Resolver.resolve(ds.getVersion(), props, model));
                String gId = URLEncoder.encode(ds.getGroupId(), "utf-8");
                String aId = URLEncoder.encode(ds.getArtifactId(), "utf-8");
                writer.write("<li>");
                writer.write("<ul class=\"list-inline\"><li><a href='");
                writer.write("?groupId=");
                writer.write(gId);
                writer.write("'>");
                writer.write(ds.getGroupId());
                writer.write("</a></li><li><a href='");
                writer.write("?groupId=");
                writer.write(gId);
                writer.write("&artifactId=");
                writer.write(aId);
                writer.write("'>");
                writer.write(ds.getArtifactId());
                writer.write("</a></li><li><a href='");
                writer.write("?groupId=");
                writer.write(gId);
                writer.write("&artifactId=");
                writer.write(aId);
                writer.write("&version=");
                writer.write(URLEncoder.encode(ds.getVersion(), "utf-8"));
                writer.write("'>");
                writer.write(ds.getVersion());
                writer.write("</a></li></ul>");
                writer.write("</li>");
              }
              writer.write("</ul>");
            }
          }
        }
      } catch (XmlPullParserException | SQLException ex) {
        throw new ServletException(ex);
      }
    }
  }

  private List<License> getLicenses(Model model, HttpServletRequest req, String repo)
      throws IOException {
    if (model.getLicenses().isEmpty()) {
      if (model.getParent() == null) {
        return model.getLicenses();
      } else {
        return getLicenses(getParent(model, req, repo), req, repo);
      }
    } else {
      return model.getLicenses();
    }
  }

  private List<Dependency> getDependencies(Model model, HttpServletRequest req, String repo)
      throws IOException {
    Map<String, Dependency> depManagement = new HashMap<>();
    fillDependencyManagement(model, req, repo, depManagement);
    for (Dependency dependency : model.getDependencies()) {
      Dependency managed =
          depManagement.get(dependency.getManagementKey());
      if (dependency.getScope() == null && managed != null) {
        dependency.setScope(managed.getScope());
      }
      if (dependency.getScope() == null) {
        dependency.setScope("compile");
      }
      if (dependency.getVersion() == null && managed != null) {
        dependency.setVersion(managed.getVersion());
      }
    }
    return model.getDependencies();
  }

  private void fillDependencyManagement(Model model, HttpServletRequest req, String repo,
      Map<String, Dependency> current) throws IOException {
    if (model.getParent() != null) {
      fillDependencyManagement(getParent(model, req, repo), req, repo, current);
    }
    if (model.getDependencyManagement() != null) {
      for (Dependency dep : model.getDependencyManagement().getDependencies()) {
        current.put(dep.getManagementKey(), dep);
      }
    }
  }
  
  private void fillProperties(Model model, HttpServletRequest req, String repo,
      Properties prop) throws IOException {
    if (model.getParent() != null) {
      fillProperties(getParent(model, req, repo), req, repo, prop);
    }
    prop.putAll(model.getProperties());
  }

  private Model getParent(Model model, HttpServletRequest req, String repo) throws IOException {
    String id = model.getParent().getGroupId() + ":" + model.getParent().getArtifactId() + ":pom:"
        + model.getParent().getVersion();

    Model parent = (Model) req.getAttribute(id);
    if (parent != null) {
      return parent;
    }

    String uri = "http://"+ req.getServerName() + ":" + req.getServerPort() + req.getRequestURI() + "repository/" +  repo + "/" + model.getParent().getGroupId().replace('.', '/') + "/"
        + model.getParent().getArtifactId() + "/" + model.getParent().getVersion() + "/"
        + model.getParent().getArtifactId() + "-" + model.getParent().getVersion() + ".pom";

    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) new URL(uri).openConnection();
      parent = new MavenXpp3Reader().read(conn.getInputStream());
      req.setAttribute(id, parent);
      return parent;
    } catch (XmlPullParserException ex) {
      throw new IOException(ex);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

}
