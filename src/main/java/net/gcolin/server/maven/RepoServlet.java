/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.gcolin.server.maven;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Maven repository servlet.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepoServlet extends HttpServlet {

    /**
     * A unique serial version identifier.
     *
     * @see Serializable#serialVersionUID
     */
    private static final long serialVersionUID = 7371970943974063406L;
    /**
     * Logger.
     */
    private static final transient Logger LOG
            = Logger.getLogger(RepoServlet.class.getName());
    /**
     * The configuration manager.
     */
    private transient ConfigurationManager configManager;
    /**
     * JAXBContext for versions.
     */
    private transient JAXBContext ctxVersion;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void init() throws ServletException {
        configManager = new ConfigurationManager();
        JmxUtil.publish("net.gcolin.server.maven:type=Configuration",
                configManager, ConfigurationJmx.class);
        try {
            ctxVersion = JAXBContext.newInstance(Version.class);
        } catch (JAXBException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void destroy() {
        JmxUtil.unpublish("net.gcolin.server.maven:type=Configuration");
        for (Repository repository
                : configManager.getConfiguration().getRepositories()) {
            JmxUtil.unpublish(ConfigurationManager.JMX_REPOSITORY
                    + repository.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void doGet(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            list(req, resp, new RepositoriesListCallback(configManager));
            return;
        }
        path = path.substring(1);

        Repository repo = checkPath(path);
        if (repo == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        path = path.substring(path.indexOf('/') + 1);

        ContentResult result = getType(req, resp, repo, path);
        if (result.isEmpty()) {
            result = getRemote(req, resp, repo, path, false,
                    Collections.EMPTY_SET);
            if (result.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                sendResult(req, resp, result);
            }
        } else {
            sendResult(req, resp, result);
        }
    }

    /**
     * Send the response to the client.
     *
     * @param req request
     * @param resp response
     * @param result result
     * @throws IOException if an error occurs
     */
    private void sendResult(final HttpServletRequest req,
            final HttpServletResponse resp,
            final ContentResult result) throws IOException {
        if (result.getChildren() != null) {
            // directory
            list(req, resp, new DirectoryListCallback(result.getChildren()));
        } else if (result.getFile() != null) {
            // a file
            String abspath = result.getFile().getAbsolutePath();

            // if the file currently downloaded,
            // we wait the completion (it is a very uncommon case)
            while (abspath.equals(configManager.getCurrentRetrieve())) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
            resp.setContentLength((int) result.getFile().length());
            FileInputStream fin = null;
            try {
                fin = new FileInputStream(result.getFile());
                copy(fin, resp.getOutputStream());
            } finally {
                close(fin);
            }
        }
    }

    /**
     * Get remote folder or file. This method is synchronized for avoiding
     * multiple retrieves of the same resource and for retrieving files one by
     * one.
     *
     * @param req request
     * @param resp response
     * @param repo repository
     * @param path path
     * @param nocheck no check if result has been downloaded before
     * @param downloaded downloaded
     * @return true if the resource exists
     * @throws IOException if an error occurs
     */
    private synchronized ContentResult getRemote(final HttpServletRequest req,
            final HttpServletResponse resp, final Repository repo,
            final String path, final boolean nocheck,
            final Set<String> downloaded) throws IOException {
        // maybe another paralle call has concluded
        ContentResult result;
        if (nocheck) {
            result = new ContentResult();
        } else {
            result = getType(req, resp, repo, path);
        }
        if (!result.isEmpty()) {
            return result;
        }
        if (isVirtual(repo)) {
            Set<String> downl = new HashSet<String>();
            for (String r : repo.getIncludes()) {
                ContentResult res
                        = getRemote(req,
                                resp,
                                configManager.getRepository(r),
                                path,
                                false,
                                downl);
                if (res.getFile() != null) {
                    return res;
                } else if (res.getChildren() != null) {
                    if (result.getChildren() == null) {
                        result.setChildren(
                                new ArrayList<File>(res.getChildren()));
                    } else {
                        result.getChildren().addAll(res.getChildren());
                    }
                }
            }
        } else if (repo.getRemote() != null) {
            String upath = repo.getRemote() + path;
            URL u = new URL(upath);
            URLConnection c = u.openConnection();
            try {
                c.setReadTimeout((int) TimeUnit.MINUTES.toMillis(10));
                c.connect();

                if (c instanceof HttpURLConnection
                        && ((HttpURLConnection) c).getResponseCode()
                        != HttpServletResponse.SC_OK) {
                    return result;
                }
                String contentType = c.getContentType();
                if (!path.endsWith(".html")
                        && !path.endsWith(".htm")
                        && contentType != null
                        && contentType.toLowerCase(Locale.ENGLISH)
                                .startsWith("text/html")) {
                    File dir = new File(configManager.getRoot(),
                            repo.getName() + File.separatorChar + path);
                    if (dir.mkdirs()) {
                        LOG.log(Level.FINE, "create directory {0}", dir);
                    }
                    result.setChildren(new ArrayList<File>());
                    ByteArrayOutputStream out = null;
                    try {
                        InputStream in = null;
                        out = new ByteArrayOutputStream();
                        try {
                            in = c.getInputStream();
                            copy(in, out);
                        } finally {
                            close(in);
                        }
                        String encoding = "utf-8";
                        String contents
                                = new String(out.toByteArray(), encoding);
                        Pattern pattern = Pattern.compile("<a.*>(.*)</a>");
                        Matcher matcher = pattern.matcher(contents);
                        while (matcher.find()) {
                            String name = matcher.group(1).trim();
                            if (!"../".equals(name)
                                    && !downloaded.contains(name)) {
                                if (downloaded != Collections.EMPTY_SET) {
                                    downloaded.add(name);
                                }
                                File file;
                                if (name.endsWith("/")) {
                                    file = new File(
                                            dir,
                                            name.substring(0,
                                                    name.length() - 1));
                                    if (file.mkdirs()) {
                                        LOG.log(Level.FINE,
                                                "create directory {0}",
                                                file);
                                    }
                                    if (new File(file, ".todo")
                                            .createNewFile()) {
                                        LOG.log(Level.FINER,
                                                "create a todo file in {0}",
                                                file);
                                    }
                                } else {
                                    String suburl;
                                    if (path.isEmpty() || path.endsWith("/")) {
                                        suburl = path + name;
                                    } else {
                                        suburl = path + "/" + name;
                                    }
                                    getRemote(req, resp, repo, suburl,
                                            false,
                                            Collections.EMPTY_SET);
                                    file = new File(dir, name);
                                }
                                result.getChildren().add(file);
                            }

                        }
                    } finally {
                        close(out);
                    }
                } else {
                    InputStream in = null;
                    FileOutputStream out = null;
                    File file = new File(configManager.getRoot(),
                            repo.getName() + File.separatorChar + path);
                    configManager.setCurrentRetrieve(file.getAbsolutePath());
                    try {
                        File parent = file.getParentFile();
                        if (parent.mkdirs()) {
                            LOG.log(Level.FINE,
                                    "create directory {0}", parent);
                        }
                        result.setFile(file);
                        in = c.getInputStream();
                        out = new FileOutputStream(file);
                        copy(in, out);
                    } finally {
                        close(in);
                        close(out);
                        configManager.setCurrentRetrieve(null);
                    }
                }
            } finally {
                if (c instanceof HttpURLConnection) {
                    ((HttpURLConnection) c).disconnect();
                }
            }
        }
        return result;
    }

    /**
     * Copy a stream in another.
     *
     * @param in the input stream
     * @param out the output stream
     * @throws IOException if an error occurs
     */
    private void copy(final InputStream in, final OutputStream out)
            throws IOException {
        byte[] b = new byte[1024];
        int count;
        while ((count = in.read(b)) != -1) {
            out.write(b, 0, count);
        }
    }

    /**
     * Close a stream.
     *
     * @param closeable stream
     */
    private void close(final Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Display a list of items.
     *
     * @param req request
     * @param resp response
     * @param cb callback
     * @throws IOException if an error occurs
     */
    private void list(final HttpServletRequest req,
            final HttpServletResponse resp,
            final ListCallback cb) throws IOException {
        Writer w = resp.getWriter();
        w.write("<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\""
                + " content=\"text/html; charset=");
        if (resp.getCharacterEncoding() == null) {
            w.write(Charset.defaultCharset().name());
        } else {
            w.write(resp.getCharacterEncoding());
        }
        w.write("\"/><title>Index of ");
        String path = req.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        w.write(path);
        w.write("</title></head><body style=\"font-family: 'Trebuchet MS', "
                + "verdana, lucida, arial, helvetica, sans-serif;\">"
                + "<h1>Index of ");
        w.write(path);
        w.write("</h1><table cellspacing=\"10\"><tr><th align=\"left\">"
                + "Name</th><th>Last Modified</th><th>Size</th></tr>");

        cb.fillTable(w);

        w.write("</table></body></html>");
    }

    /**
     * Get the type of resource.
     *
     * @param req request
     * @param resp response
     * @param repo repository
     * @param path relative path
     * @return -1 unknown 1 : directory 0 : file
     * @throws IOException if an i/o error occurs
     */
    private ContentResult getType(final HttpServletRequest req,
            final HttpServletResponse resp, final Repository repo,
            final String path) throws IOException {
        ContentResult result = new ContentResult();
        if (isVirtual(repo)) {
            for (String r : repo.getIncludes()) {
                ContentResult res = getType(req, resp,
                        configManager.getRepository(r), path);
                if (res.getFile() != null) {
                    return res;
                } else if (res.getChildren() != null) {
                    if (result.getChildren() == null) {
                        result.setChildren(
                                new ArrayList<File>(res.getChildren()));
                    } else {
                        result.getChildren().addAll(res.getChildren());
                    }
                }
            }
        } else {
            File file = new File(configManager.getRoot(), repo.getName()
                    + File.separatorChar + path);
            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        result.setChildren(Arrays.asList(children));
                    } else {
                        result.setChildren(Collections.EMPTY_LIST);
                    }
                    if (result.getChildren().size() == 1
                            && result.getChildren().get(0).getName()
                                    .endsWith(".todo")) {
                        if (result.getChildren().get(0).delete()) {
                            LOG.log(Level.FINER,
                                    "remove todo file in {0}", file);
                        }
                        result.setChildren(
                                getRemote(req, resp, repo, path, true,
                                        Collections.EMPTY_SET).getChildren());
                    }
                } else {
                    result.setFile(file);
                }
            }
        }
        return result;
    }

    /**
     * Check if a repository is a virtual repository.
     *
     * @param repo repository
     * @return true if the repository is virtual
     */
    private boolean isVirtual(final Repository repo) {
        return repo.getIncludes() != null && !repo.getIncludes().isEmpty();
    }

    /**
     * Get a repository with the request path.
     *
     * @param path request path
     * @return null or a repository
     */
    private Repository checkPath(final String path) {
        int n = path.indexOf('/');
        if (n == -1) {
            return null;
        } else {
            return configManager.getRepository(path.substring(0, n));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void doPut(final HttpServletRequest req,
            final HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getPathInfo().substring(1);
        Repository repo = checkPath(path);
        if (repo == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File file = new File(configManager.getRoot(), path);
        File parent = file.getParentFile();
        if (parent.mkdirs()) {
            LOG.log(Level.FINE, "create directory {0}", file);
        }

        OutputStream fout = null;
        try {
            fout = new FileOutputStream(file);
            copy(req.getInputStream(), fout);
        } finally {
            close(fout);
        }
        if (parent.getName().endsWith("-SNAPSHOT")
                && file.getName().equals("maven-metadata.xml")) {
            CleanUp.cleanUpSnapshots(file, configManager.getConfiguration(),
                    ctxVersion);
        }
    }

}