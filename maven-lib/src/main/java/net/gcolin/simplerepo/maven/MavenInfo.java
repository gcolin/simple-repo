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
package net.gcolin.simplerepo.maven;

import net.gcolin.simplerepo.model.ResultType;
import net.gcolin.simplerepo.model.SearchResult;
import net.gcolin.simplerepo.util.ConfigurationManager;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Extract Maven information.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class MavenInfo {

  private static final String[] SCOPES =
      new String[] {"compile", "runtime", "test", "system", "provided"};

  private static final String[] SCOPES_TITLE =
      new String[] {"Compile", "Runtime", "Test", "System", "Provided"};

  private MavenInfo() {}

  public static void writeHtml(Writer writer, SearchResult result,
      ConfigurationManager configManager, MavenUtil mavenUtil, String baseFilePath,
      String breadcrumbUrl, String javadoc, DisplayLink displayLink) throws IOException {

    Map<String, Model> cache = new HashMap<>();

    writer.write("<ol class=\"breadcrumb\"><li><a href='");
    writer.write(breadcrumbUrl);
    writer.write("?groupId=");
    writer.write(URLEncoder.encode(result.getGroupId(), "utf-8"));
    writer.write("'>");
    writer.write(result.getGroupId());
    writer.write("</a></li><li><a href='");
    writer.write(breadcrumbUrl);
    writer.write("?groupId=");
    writer.write(URLEncoder.encode(result.getGroupId(), "utf-8"));
    writer.write("&artifactId=");
    writer.write(URLEncoder.encode(result.getArtifactId(), "utf-8"));
    writer.write("'>");
    writer.write(result.getArtifactId());
    writer.write("</a></li><li class=\"active\">");
    writer.write(result.getVersion());
    writer.write("</li></ol>");
    File pom = new File(configManager.getRoot(),
        result.getRepoName() + "/" + result.getGroupId().replace('.', '/') + "/"
            + result.getArtifactId() + "/" + result.getVersion() + "/" + result.getArtifactId()
            + "-" + result.getVersion() + ".pom");
    Model model = mavenUtil.readPom(pom);

    Properties props = new Properties();
    fillProperties(model, configManager, result.getRepoName(), cache, props);

    writer.write("<h1>");
    writer.write(Resolver.resolve(model.getName(), props, model));
    writer.write("</h1>");
    List<License> licenses = getLicenses(model, configManager, result.getRepoName(), cache);
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
    writer.write(new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH).format(pom.lastModified()));
    writer.write("</i></p>");
    if (model.getUrl() != null) {
      writer.write("<p>");
      writer.write(" <a target='_blank' href='");
      writer.write(model.getUrl());
      writer.write("'>");
      writer.write(model.getUrl());
      writer.write("</a></p>");
    }
    if (javadoc != null) {
      writer.write("<p class='text-info'>");
      writer.write(" <a target='_blank' href='");
      writer.write(javadoc);
      writer.write("'>Javadoc</a></p>");
    }

    writer.write("<p>");
    if (model.getDescription() != null) {
      writer.write(model.getDescription());
    }
    writer.write("</p>");

    writer.write("<ul class=\"nav nav-tabs\" id=\"code\" role=\"tablist\">");
    writer.write(
        "<li role=\"presentation\" class=\"active\"><a href=\"#maven\" id=\"maven-tab\" role=\"tab\" data-toggle=\"tab\" aria-controls=\"maven\" aria-expanded=\"true\">Maven</a></li>");
    for (String name : new String[] {"Gradle", "SBT", "Ivy", "Grape", "Leiningen", "Buildr"}) {
      writer.write(MessageFormat.format(
          "<li role=\"presentation\"><a href=\"#{1}\" role=\"tab\" id=\"{1}-tab\" data-toggle=\"tab\" aria-controls=\"{1}\">{0}</a></li>",
          name, name.toLowerCase()));
    }
    writer.write(
        "</ul><div class=\"tab-content\" id=\"codeContent\"><div class=\"tab-pane fade in active\" role=\"tabpanel\" id=\"maven\" aria-labelledby=\"maven-tab\"><pre>");
    writer.write("\n&lt;dependency&gt;");
    writer.write("\n    &lt;groupId&gt;");
    writer.write(result.getGroupId());
    writer.write("&lt;/groupId&gt;");
    writer.write("\n    &lt;artifactId&gt;");
    writer.write(result.getArtifactId());
    writer.write("&lt;/artifactId&gt;");
    writer.write("\n    &lt;version&gt;");
    writer.write(result.getVersion());
    writer.write("&lt;/version&gt;");
    writer.write("\n&lt;/dependency&gt;</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"gradle\" aria-labelledby=\"gradle-tab\"><pre>");
    writer.write("\ncompile group: '");
    writer.write(result.getGroupId());
    writer.write("', name: '");
    writer.write(result.getArtifactId());
    writer.write("', version: '");
    writer.write(result.getVersion());
    writer.write("'</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"sbt\" aria-labelledby=\"sbt-tab\"><pre>");
    writer.write("\nlibraryDependencies += \"");
    writer.write(result.getGroupId());
    writer.write("\" % \"");
    writer.write(result.getArtifactId());
    writer.write("\" % \"");
    writer.write(result.getVersion());
    writer.write("\"</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"ivy\" aria-labelledby=\"ivy-tab\"><pre>");
    writer.write("\n&lt;dependency org=\"");
    writer.write(result.getGroupId());
    writer.write("\" name=\"");
    writer.write(result.getArtifactId());
    writer.write("\" rev=\"");
    writer.write(result.getVersion());
    writer.write("\"/&gt;</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"grape\" aria-labelledby=\"grape-tab\"><pre>");
    writer.write("\n@Grapes(");
    writer.write("\n    @Grab(group='");
    writer.write(result.getGroupId());
    writer.write("', module='");
    writer.write(result.getArtifactId());
    writer.write("', version='");
    writer.write(result.getVersion());
    writer.write("')\n)</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"leiningen\" aria-labelledby=\"leiningen-tab\"><pre>");
    writer.write("\n[");
    writer.write(result.getGroupId());
    writer.write("/");
    writer.write(result.getArtifactId());
    writer.write(" \"");
    writer.write(result.getVersion());
    writer.write("\"]</pre></div>");
    writer.write(
        "<div class=\"tab-pane fade\" role=\"tabpanel\" id=\"buildr\" aria-labelledby=\"buildr-tab\"><pre>");
    writer.write("\n'");
    writer.write(result.getGroupId());
    writer.write(":");
    writer.write(result.getArtifactId());
    writer.write(":");
    writer.write(model.getPackaging());
    writer.write(":");
    writer.write(result.getVersion());
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
      writer.write("<li><a target='_blank' href='");
      writer.write(baseFilePath);
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
    List<Dependency> dependencies =
        getDependencies(model, configManager, result.getRepoName(), cache);
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
          boolean hasLink =
              displayLink.isAvailable(ds.getGroupId(), ds.getArtifactId(), ds.getVersion());
          if (hasLink) {
            String gId = URLEncoder.encode(ds.getGroupId(), "utf-8");
            String aId = URLEncoder.encode(ds.getArtifactId(), "utf-8");
            writer.write("<li>");
            writer
                .write("<ul class=\"list-inline\" style=\"display: inline-block;\"><li><a href='");
            writer.write(breadcrumbUrl);
            writer.write("?groupId=");
            writer.write(gId);
            writer.write("'>");
            writer.write(ds.getGroupId());
            writer.write("</a></li><li><a href='");
            writer.write(breadcrumbUrl);
            writer.write("?groupId=");
            writer.write(gId);
            writer.write("&artifactId=");
            writer.write(aId);
            writer.write("'>");
            writer.write(ds.getArtifactId());
            String directlink = displayLink.getExactLink(ds.getGroupId(), ds.getArtifactId(), ds.getVersion());
            writer.write("</a></li><li><a href='");
            if(directlink != null) {
              writer.write(directlink);
            } else {
              writer.write(breadcrumbUrl);
              writer.write("?groupId=");
              writer.write(gId);
              writer.write("&artifactId=");
              writer.write(aId);
              writer.write("&version=");
              writer.write(URLEncoder.encode(ds.getVersion(), "utf-8"));
            }
            writer.write("'>");
            writer.write(ds.getVersion());
            writer.write("</a></li></ul>");
            writer.write("</li>");
          } else {
            writer.write("<li>");
            writer.write("<ul class=\"list-inline\" style=\"display: inline-block;\"><li>");
            writer.write(ds.getGroupId());
            writer.write("</li><li>");
            writer.write(ds.getArtifactId());
            writer.write("</li><li>");
            writer.write(ds.getVersion());
            writer.write("</li></ul>");
            writer.write("</li>");
          }
        }
        writer.write("</ul>");
      }
    }

    Set<Developer> dev = new TreeSet<>(new Comparator<Developer>() {

      @Override
      public int compare(Developer o1, Developer o2) {
        if (o1.getName() == null) {
          return -1;
        }
        if (o2.getName() == null) {
          return 1;
        }
        int comp = o1.getName().compareTo(o2.getName());
        if (comp != 0) {
          return comp;
        }
        if (o1.getId() == null) {
          return -1;
        }
        if (o2.getId() == null) {
          return 1;
        }
        comp = o1.getId().compareTo(o2.getId());
        if (comp != 0) {
          return comp;
        }
        if (o1.getEmail() == null) {
          return -1;
        }
        if (o2.getEmail() == null) {
          return 1;
        }
        return o1.getEmail().compareTo(o2.getEmail());
      }

    });
    fillDevelopper(model, configManager, result.getRepoName(), cache, dev);

    if (!dev.isEmpty()) {
      writer.write("<h3>Developers (");
      writer.write(String.valueOf(dev.size()));
      writer.write(
          ")</h3><ul><table class='table'><tr><th>Name</th><th>Id</th><th>Email</th><th>Organization</th></tr>");
      for (Developer d : dev) {
        writer.write("<tr><td>");
        if (d.getName() != null) {
          writer.write(d.getName());
        }
        writer.write("</td><td>");
        if (d.getId() != null) {
          writer.write(d.getId());
        }
        writer.write("</td><td>");
        if (d.getEmail() != null) {
          writer.write(d.getEmail());
        }
        writer.write("</td><td>");
        if (d.getOrganization() != null) {
          if (d.getOrganizationUrl() != null) {
            writer.write("<a target='_blank' href='");
            writer.write(d.getOrganizationUrl());
            writer.write("'>");
          }
          writer.write(d.getOrganization());
          if (d.getOrganizationUrl() != null) {
            writer.write("</a>");
          }
        }
        writer.write("</td></tr>");
      }
      writer.write("</table>");

    }
  }

  private static void fillProperties(Model model, ConfigurationManager configManager, String repo,
      Map<String, Model> cache, Properties prop) throws IOException {
    if (model.getParent() != null) {
      fillProperties(getParent(model, configManager, repo, cache), configManager, repo, cache,
          prop);
    }
    prop.putAll(model.getProperties());
  }

  private static List<License> getLicenses(Model model, ConfigurationManager configManager,
      String repo, Map<String, Model> cache) throws IOException {
    if (model.getLicenses().isEmpty()) {
      if (model.getParent() == null) {
        return model.getLicenses();
      } else {
        return getLicenses(getParent(model, configManager, repo, cache), configManager, repo,
            cache);
      }
    } else {
      return model.getLicenses();
    }
  }

  private static List<Dependency> getDependencies(Model model, ConfigurationManager configManager,
      String repo, Map<String, Model> cache) throws IOException {
    Map<String, Dependency> depManagement = new HashMap<>();
    fillDependencyManagement(model, configManager, repo, cache, depManagement);
    for (Dependency dependency : model.getDependencies()) {
      Dependency managed = depManagement.get(dependency.getManagementKey());
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

  private static void fillDependencyManagement(Model model, ConfigurationManager configManager,
      String repo, Map<String, Model> cache, Map<String, Dependency> current) throws IOException {
    if (model.getParent() != null) {
      fillDependencyManagement(getParent(model, configManager, repo, cache), configManager, repo,
          cache, current);
    }
    if (model.getDependencyManagement() != null) {
      for (Dependency dep : model.getDependencyManagement().getDependencies()) {
        current.put(dep.getManagementKey(), dep);
      }
    }
  }

  private static void fillDevelopper(Model model, ConfigurationManager configManager, String repo,
      Map<String, Model> cache, Set<Developer> all) throws IOException {
    if (model.getParent() != null) {
      fillDevelopper(getParent(model, configManager, repo, cache), configManager, repo, cache, all);
    }
    if (model.getDevelopers() != null) {
      all.addAll(model.getDevelopers());
    }
  }

  private static Model getParent(Model model, ConfigurationManager configManager, String repo,
      Map<String, Model> cache) throws IOException {
    String id = model.getParent().getGroupId() + ":" + model.getParent().getArtifactId() + ":pom:"
        + model.getParent().getVersion();

    Model parent = (Model) cache.get(id);
    if (parent != null) {
      return parent;
    }

    String uri = pomUrl(model, configManager, repo);

    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) new URL(uri).openConnection();
      if (conn.getResponseCode() != 404) {
        parent = new MavenXpp3Reader().read(conn.getInputStream());
        cache.put(id, parent);
        return parent;
      }
    } catch (XmlPullParserException ex) {
      throw new IOException(ex);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }

    uri = pomUrl(model, configManager, "public");
    try {
      conn = (HttpURLConnection) new URL(uri).openConnection();
      parent = new MavenXpp3Reader().read(conn.getInputStream());
      cache.put(id, parent);
      return parent;
    } catch (XmlPullParserException ex) {
      throw new IOException(ex);
    } finally {
      if (conn != null) {
        conn.disconnect();
      }
    }
  }

  private static String pomUrl(Model model, ConfigurationManager configManager, String repo) {
    return configManager.getServerBaseUrl() + "repository/" + repo + "/"
        + model.getParent().getGroupId().replace('.', '/') + "/" + model.getParent().getArtifactId()
        + "/" + model.getParent().getVersion() + "/" + model.getParent().getArtifactId() + "-"
        + model.getParent().getVersion() + ".pom";
  }

}
