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
package net.gcolin.simplerepo.extract;

import net.gcolin.simplerepo.bootstrap.BootstrapListener;
import net.gcolin.simplerepo.maven.MavenInfo;
import net.gcolin.simplerepo.maven.MavenUtil;
import net.gcolin.simplerepo.model.SearchResult;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.Io;

import org.apache.maven.model.Model;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.listener.CommonsLoggingListener;
import org.apache.tools.ant.taskdefs.Sync;
import org.apache.tools.ant.types.FileSet;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Generate website.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class ExtractController extends MavenUtil {

  private final static String EXTRACT = "Extract repository: ";
  private ConfigurationManager configurationMananger;
  private volatile boolean running = false;
  private int nb = 0;
  private static final String[] FILES = {"script.js", "jquery/jquery-3.1.0.min.js",
      "bootstrap/js/bootstrap.min.js", "bootstrap/fonts/glyphicons-halflings-regular.eot",
      "bootstrap/fonts/glyphicons-halflings-regular.svg",
      "bootstrap/fonts/glyphicons-halflings-regular.ttf",
      "bootstrap/fonts/glyphicons-halflings-regular.woff",
      "bootstrap/fonts/glyphicons-halflings-regular.woff2"};

  public ExtractController(ConfigurationManager configurationMananger) {
    this.configurationMananger = configurationMananger;
  }

  public void extract() {
    if (running) {
      return;
    }
    running = true;
    try {
      configurationMananger.setCurrentAction(EXTRACT + "initialize");
      String repoName = configurationMananger.getProperty("config.extractedRepository");
      if (repoName == null) {
        repoName = "releases";
      }
      File repo = new File(configurationMananger.getRoot(), "plugins/" + repoName);
      configurationMananger.setCurrentAction(EXTRACT + "copy files");
      Project p = new Project();
      p.addBuildListener(new CommonsLoggingListener());
      p.setBaseDir(configurationMananger.getRoot());
      Target target = new Target();
      target.setProject(p);
      target.setName("build");
      Sync sync = new Sync();
      sync.setProject(p);
      sync.init();
      sync.setTodir(new File(repo, "public"));
      FileSet fileSet = new FileSet();
      fileSet.setDir(new File(configurationMananger.getRoot(), repoName));
      sync.addFileset(fileSet);
      target.addTask(sync);
      p.addTarget(target);
      p.init();
      p.executeTarget("build");
      p.fireBuildFinished(null);
      buildIndex(repo, configurationMananger, repoName);
      buildData(repo, configurationMananger, repoName);
      copyScript(repo, configurationMananger);
    } catch (IOException ex) {
      Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "cannot extract repository",
          ex);
    } finally {
      configurationMananger.setCurrentAction(null);
      running = false;
    }
  }

  private void buildData(final File dir, final ConfigurationManager configManager,
      final String repoName) throws IOException {
    if (dir.exists()) {
      final boolean javadoc =
          Boolean.parseBoolean(configManager.getProperty("config.extractJavadoc"))
              || "on".equals(configManager.getProperty("config.extractJavadoc"));
      try (final Writer swriter =
          new OutputStreamWriter(new FileOutputStream(new File(dir, "data.js")))) {
        swriter.write("window.data = [");
        nb = 0;
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (file.toString().endsWith(".pom")) {
              Model model = readPom(file.toFile());
              SearchResult result = buildResult(repoName, file.toFile(), model);
              File javadocDir = new File(dir, result.getGroupId()
                  + "-" + result.getArtifactId() + "-" + result.getVersion());
              if (javadoc) {
                String path = file.toFile().getAbsolutePath();
                File javadocJar = new File(path.substring(0, path.length() - 4) + "-javadoc.jar");
                if (javadocJar.exists()) {
                  javadocDir.mkdir();
                  Io.unzip(javadocJar, javadocDir);
                }
              }
              if (nb > 0) {
                swriter.write(",");
              }
              swriter.write("{g:\"");
              swriter.write(result.getGroupId());
              swriter.write("\",a:\"");
              swriter.write(result.getArtifactId());
              swriter.write("\",v:\"");
              swriter.write(result.getVersion());
              swriter.write("\"}");
              nb++;
              try (Writer writer =
                  new OutputStreamWriter(new FileOutputStream(new File(dir, result.getGroupId()
                      + "-" + result.getArtifactId() + "-" + result.getVersion() + ".html")))) {

                BootstrapListener theme = new BootstrapListener();

                HttpServletRequest req = writeStartHtml(configManager, repoName, writer, theme);
                writer.append("</head><body>");
                theme.onStartBody(req, writer);
                writer.write(
                    "<form id='search-form' action='index.html'><div class=\"input-group\">");
                writer.write(
                    "<input type=\"text\" name=\"q\" class=\"form-control\" placeholder=\"Search for...\" value=\"");
                writer.write("\">");
                writer.write("<span class=\"input-group-btn\">");
                writer.write(
                    "<button class=\"btn btn-default\" type=\"submit\" rel=\"no-follow\"><span class=\"glyphicon glyphicon-search\"></span> Go!</button>");
                writer.write("</span></div></form>");
                File javadocIndex = new File(javadocDir, "index.html");
                MavenInfo.writeHtml(writer, result, configManager, ExtractController.this,
                    "public/", "index.html", javadocIndex.exists() ? javadocDir.getName() + "/index.html" : null);
                theme.onEndBody(req, writer);
                writer.append("</body></html>");
              }

            }
            return FileVisitResult.CONTINUE;
          }

        });
        swriter.write("];");
      }
    }
  }

  private HttpServletRequest writeStartHtml(final ConfigurationManager configManager,
      final String repoName, Writer writer, BootstrapListener theme) throws IOException {
    writer.append("<html><head><title>Simple repo</title>"
        + "<meta charset=\"UTF-8\"><meta name=\"viewport\" "
        + "content=\"width=device-width, initial-scale=1.0\">");

    HttpServletRequest req = fakeRequest();

    String title = configManager.getProperty("config.extractTitle");
    if (title == null) {
      title = repoName;
    }
    req.setAttribute("title", title);
    req.setAttribute("base", ".");
    req.setAttribute("theme", configManager.getProperty("config.extractTheme"));


    req.setAttribute("menu", new LinkedHashMap<>());
    theme.onHead(req, writer);
    return req;
  }

  public void copyScript(final File dir, ConfigurationManager configManager) throws IOException {
    for (String fileName : FILES) {
      copy0(dir, fileName);
    }
    String theme = configManager.getProperty("config.extractTheme");
    if (theme == null) {
      theme = "default";
    } else {
      theme = theme.toLowerCase();
    }
    copy0(dir, "bootstrap/css/" + theme + ".css");
  }

  private void copy0(final File dir, String fileName) throws IOException, FileNotFoundException {
    File file = new File(dir, "resources/" + fileName);
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      try (OutputStream out = new FileOutputStream(file)) {
        try (InputStream in = this.getClass().getClassLoader()
            .getResourceAsStream("META-INF/resources/" + fileName)) {
          IOUtil.copy(in, out);
        }
      }
    }
  }

  private void buildIndex(File dir, ConfigurationManager configManager, String repoName)
      throws IOException {
    try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, "index.html")),
        StandardCharsets.UTF_8)) {
      BootstrapListener theme = new BootstrapListener();

      HttpServletRequest req = writeStartHtml(configManager, repoName, writer, theme);

      writer.append("<script type='text/javascript' src=\"data.js\"></script>");
      writer.append("<script type='text/javascript' src=\"./resources/script.js\"></script>");
      writer.append("</head><body>");
      theme.onStartBody(req, writer);
      writer.write("<form id='search-form'><div class=\"input-group\">");
      writer.write(
          "<input type=\"text\" id=\"q\" name=\"q\" class=\"form-control\" placeholder=\"Search for...\" value=\"");
      writer.write("\">");
      writer.write("<span class=\"input-group-btn\">");
      writer.write(
          "<button class=\"btn btn-default\" type=\"submit\" rel=\"no-follow\"><span class=\"glyphicon glyphicon-search\"></span> Go!</button>");
      writer.write("</span></div></form><div id='content'></div>");
      theme.onEndBody(req, writer);
      writer.append("</body></html>");
    }
  }

  private HttpServletRequest fakeRequest() {
    HttpServletRequest req =
        (HttpServletRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[] {HttpServletRequest.class}, new InvocationHandler() {

              private Map<Object, Object> attrs = new HashMap<>();

              @Override
              public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getName().equals("setAttribute")) {
                  attrs.put(args[0], args[1]);
                } else if (method.getName().equals("getAttribute")) {
                  return attrs.get(args[0]);
                } else if (method.getName().equals("removeAttribute")) {
                  return attrs.remove(args[0]);
                }
                return null;
              }
            });
    return req;
  }

}
