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
package net.gcolin.simplerepo.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Base test file.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class AbstractRepoTest {

  protected Server createServer(int port, String displayName) throws Exception {
    FileUtils.deleteDirectory(new File("target/repo" + displayName));
    System.setProperty("simplerepo.root", "target/repo" + displayName);
    Server server = new Server(port);
    FileUtils.copyDirectory(new File("src/main/webapp"), new File("target/server"));
    WebAppContext app = new WebAppContext("target/server", "/simple-repo");
    app.setAttribute("contextName", displayName);
    app.setExtractWAR(true);
    app.getSecurityHandler()
        .setLoginService(new HashLoginService("Test realm", "src/test/resources/realm.properties"));
    app.setDisplayName(displayName);
    server.setHandler(app);
    server.start();
    return server;
  }

  protected Object executeOperationJmx(String name, String operation, Object[] arguments,
      String[] signature) throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName oname = server.queryNames(new ObjectName(name), null).iterator().next();
    return server.invoke(oname, operation, arguments, signature);
  }

  protected void setAttributeJmx(String name, String attribute, Object arguments) throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName oname = server.queryNames(new ObjectName(name), null).iterator().next();
    server.setAttribute(oname, new Attribute(attribute, arguments));
  }

  protected void addRepository(String contextName, String repositoryName, String remote)
      throws Exception {
    executeOperationJmx("net.gcolin.simplerepo:ctx=" + contextName + ",type=Configuration",
        "newRepository", new Object[] {repositoryName}, new String[] {"java.lang.String"});
    if (remote != null) {
      executeOperationJmx(
          "net.gcolin.simplerepo:ctx=" + contextName + ",type=Repository,name=" + repositoryName,
          "updateRemoteUrl", new Object[] {remote}, new String[] {"java.lang.String"});
    }
  }

  protected void setArtifactMaxAge(String contextName, String repositoryName, long maxAge)
      throws Exception {
    executeOperationJmx(
        "net.gcolin.simplerepo:ctx=" + contextName + ",type=Repository,name=" + repositoryName,
        "updateArtifactMaxAge", new Object[] {maxAge}, new String[] {"long"});
  }

  protected void setNotFoundCache(String contextName, long maxAge) throws Exception {
    setAttributeJmx("net.gcolin.simplerepo:ctx=" + contextName + ",type=Configuration",
        "NotFoundCache", maxAge);
  }

  protected int getStatus(String url, long lastUpdate) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    try {
      if (lastUpdate > 0) {
        c.setIfModifiedSince(lastUpdate);
      }
      c.setUseCaches(false);
      c.connect();
      return c.getResponseCode();
    } finally {
      c.disconnect();
    }
  }

  protected String getContent(String url, long lastUpdate) throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    try {
      if (lastUpdate > 0) {
        c.setIfModifiedSince(lastUpdate);
      }
      c.setUseCaches(false);
      c.connect();
      return c.getResponseCode() == HttpURLConnection.HTTP_OK
          ? new String(IOUtils.toByteArray(c.getInputStream()), "utf-8") : null;
    } finally {
      c.disconnect();
    }
  }

  protected int sendContent(String url, String fileContent, String user, String password)
      throws IOException {
    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
    try {
      c.setDoOutput(true);
      c.setRequestMethod("PUT");
      if (user != null) {
        c.setRequestProperty("Authorization",
            "Basic " + Base64.encodeBase64String((user + ":" + password).getBytes("utf-8")));
      }
      c.connect();
      IOUtils.copy(new ByteArrayInputStream(fileContent.getBytes("utf-8")), c.getOutputStream());
      return c.getResponseCode();
    } finally {
      c.disconnect();
    }
  }
}
