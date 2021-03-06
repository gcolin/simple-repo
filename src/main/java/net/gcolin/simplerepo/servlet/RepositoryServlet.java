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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.gcolin.simplerepo.model.ContentResult;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;
import net.gcolin.simplerepo.util.DirectoryListCallback;
import net.gcolin.simplerepo.util.Io;
import net.gcolin.simplerepo.util.ListCallback;
import net.gcolin.simplerepo.util.RepositoriesListCallback;

/**
 * Maven repository servlet.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepositoryServlet extends AbstractDisplayServlet {

	/**
	 * A unique serial version identifier.
	 *
	 * @see Serializable#serialVersionUID
	 */
	private static final long serialVersionUID = 7371970943974063406L;
	/**
	 * The configuration manager.
	 */
	private transient ConfigurationManager configManager;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void init() throws ServletException {
		configManager = (ConfigurationManager) getServletContext().getAttribute("configManager");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp)
			throws ServletException, IOException {
		String path = req.getPathInfo();
		if (path == null || path.isEmpty()) {
			resp.sendRedirect(req.getServletPath().substring(1) + "/");
			return;
		}
		if ("/".equals(path)) {
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
			result = getRemote(req, resp, repo, path, false, Collections.EMPTY_SET, null);
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
	 * @param req    request
	 * @param resp   response
	 * @param result result
	 * @throws IOException if an error occurs
	 * @throws ServletException 
	 */
	private void sendResult(final HttpServletRequest req, final HttpServletResponse resp, final ContentResult result)
			throws IOException, ServletException {
		if (result.getChildren() != null) {
			// directory
			list(req, resp, new DirectoryListCallback(result.getChildren()));
		} else if (result.getFile() != null) {
			// a file
			Enumeration<String> en = req.getHeaders("If-Modified-Since");
			if (en.hasMoreElements()) {
				long date = req.getDateHeader("If-Modified-Since");
				if (result.getFile().lastModified() <= date) {
					resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}
			}
			resp.setContentLength((int) result.getFile().length());
			resp.setDateHeader("Last-Modified", result.getFile().lastModified());
			FileInputStream fin = null;
			try {
				fin = new FileInputStream(result.getFile());
				Io.copy(fin, resp.getOutputStream());
			} finally {
				Io.close(fin);
			}
		}
	}

	/**
	 * Get remote folder or file. This method is synchronized for avoiding multiple
	 * retrieves of the same resource and for retrieving files one by one.
	 *
	 * @param req        request
	 * @param resp       response
	 * @param repo       repository
	 * @param path       path
	 * @param nocheck    no check if result has been downloaded before
	 * @param downloaded downloaded
	 * @return true if the resource exists
	 * @throws IOException if an error occurs
	 */
	@SuppressWarnings("unchecked")
	private synchronized ContentResult getRemote(final HttpServletRequest req, final HttpServletResponse resp,
			final Repository repo, final String path, final boolean nocheck, final Set<String> downloaded,
			File previous) throws IOException {
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
				ContentResult res = getRemote(req, resp, configManager.getRepository(r), path, false, downl, null);
				if (res.getFile() != null) {
					return res;
				} else if (res.getChildren() != null) {
					if (result.getChildren() == null) {
						result.setChildren(new ArrayList<File>(res.getChildren()));
					} else {
						result.getChildren().addAll(res.getChildren());
					}
				}
			}
		} else if (repo.getRemote() != null) {
			File file = new File(configManager.getRoot(), repo.getName() + File.separatorChar + path);
			File parent = file.getParentFile();
			if (parent.mkdirs()) {
				configManager.getLogger().log(Level.FINE, "create directory {0}", parent);
			}

			File notfound = new File(file.getParentFile(), file.getName() + ".notfound");
			if (notfound.exists()) {
				if ((System.currentTimeMillis() - notfound.lastModified()) <= configManager.getNotFoundCache()) {
					return result;
				} else {
					if (notfound.delete()) {
						configManager.getLogger().log(Level.FINE, "delete file {0}", notfound);
					}
				}
			}

			String upath = repo.getRemote() + path;
			URL u = new URL(upath);
			URLConnection c = u.openConnection();
			c.setUseCaches(false);
			try {
				if (previous != null) {
					c.setIfModifiedSince(previous.lastModified());
				}
				c.setReadTimeout((int) TimeUnit.MINUTES.toMillis(10));
				c.connect();

				if (c instanceof HttpURLConnection) {
					int statusCode = ((HttpURLConnection) c).getResponseCode();
					if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
						result.setFile(previous);
						return result;
					} else if (statusCode != HttpServletResponse.SC_OK) {
						if (notfound.createNewFile()) {
							configManager.getLogger().log(Level.FINE, "create file {0}", notfound);
						}
						return result;
					}
				}
				String contentType = c.getContentType();
				if (!path.endsWith(".html") && !path.endsWith(".htm") && contentType != null
						&& contentType.toLowerCase(Locale.ENGLISH).startsWith("text/html")) {
					if (file.mkdirs()) {
						configManager.getLogger().log(Level.FINE, "create directory {0}", file);
					}
					result.setChildren(new ArrayList<File>());
					ByteArrayOutputStream out = null;
					try {
						InputStream in = null;
						out = new ByteArrayOutputStream();
						try {
							in = c.getInputStream();
							Io.copy(in, out);
						} finally {
							Io.close(in);
						}
						String encoding = "utf-8";
						String contents = new String(out.toByteArray(), encoding);
						Pattern pattern = Pattern.compile("<a.*>(.*)</a>");
						Matcher matcher = pattern.matcher(contents);
						while (matcher.find()) {
							String name = matcher.group(1).trim();
							if (!"../".equals(name) && !downloaded.contains(name)) {
								if (downloaded != Collections.EMPTY_SET) {
									downloaded.add(name);
								}
								File subfile;
								if (name.endsWith("/")) {
									subfile = new File(file, name.substring(0, name.length() - 1));
									if (subfile.mkdir()) {
										configManager.getLogger().log(Level.FINE, "create directory {0}", subfile);
									}
									if (new File(subfile, ".todo").createNewFile()) {
										configManager.getLogger().log(Level.FINER, "create a todo file in {0}", file);
									}
								} else {
									String suburl;
									if (path.isEmpty() || path.endsWith("/")) {
										suburl = path + name;
									} else {
										suburl = path + "/" + name;
									}
									getRemote(req, resp, repo, suburl, false, Collections.EMPTY_SET, null);
									subfile = new File(file, name);
								}
								result.getChildren().add(subfile);
							}

						}
					} finally {
						Io.close(out);
					}
				} else {
					InputStream in = null;
					FileOutputStream out = null;
					try {
						result.setFile(file);
						in = c.getInputStream();
						out = new FileOutputStream(file);
						Io.copy(in, out);
					} finally {
						Io.close(in);
						Io.close(out);
						long lastModified = c.getLastModified();
						if (lastModified > 0) {
							file.setLastModified(lastModified);
						}
					}
				}
			} finally {
				if (c instanceof HttpURLConnection) {
					((HttpURLConnection) c).disconnect();
				}
			}
		} else if (path.length() == 0) {
			result.setChildren(Collections.EMPTY_LIST);
		}
		return result;
	}

	/**
	 * Display a list of items.
	 *
	 * @param req  request
	 * @param resp response
	 * @param cb   callback
	 * @throws IOException if an error occurs
	 * @throws ServletException 
	 */
	private void list(final HttpServletRequest req, final HttpServletResponse resp, final ListCallback cb)
			throws IOException, ServletException {
		req.setAttribute("cb", cb);
		String path = req.getPathInfo();
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		req.setAttribute("title", "Index of " + path);
		super.doGet(req, resp);
	}

	/**
	 * Get the type of resource.
	 *
	 * @param req  request
	 * @param resp response
	 * @param repo repository
	 * @param path relative path
	 * @return -1 unknown 1 : directory 0 : file
	 * @throws IOException if an i/o error occurs
	 */
	@SuppressWarnings("unchecked")
	private ContentResult getType(final HttpServletRequest req, final HttpServletResponse resp, final Repository repo,
			final String path) throws IOException {
		ContentResult result = new ContentResult();
		if (isVirtual(repo)) {
			for (String r : repo.getIncludes()) {
				ContentResult res = getType(req, resp, configManager.getRepository(r), path);
				if (res.getFile() != null) {
					return res;
				} else if (res.getChildren() != null) {
					if (result.getChildren() == null) {
						result.setChildren(new ArrayList<File>(res.getChildren()));
					} else {
						result.getChildren().addAll(res.getChildren());
					}
				}
			}
		} else {
			File file = new File(configManager.getRoot(), repo.getName() + File.separatorChar + path);
			if (file.exists()) {
				if (file.isDirectory()) {
					File[] children = file.listFiles();
					if (children != null) {
						result.setChildren(Arrays.asList(children));
					} else {
						result.setChildren(Collections.EMPTY_LIST);
					}
					if (result.getChildren().size() == 1 && result.getChildren().get(0).getName().endsWith(".todo")) {
						if (result.getChildren().get(0).delete()) {
							configManager.getLogger().log(Level.FINER, "remove todo file in {0}", file);
						}
						result.setChildren(
								getRemote(req, resp, repo, path, true, Collections.EMPTY_SET, null).getChildren());
					}
				} else if (repo.getArtifactMaxAge() == -1
						|| (System.currentTimeMillis() - file.lastModified()) <= repo.getArtifactMaxAge()) {
					result.setFile(file);
				} else {
					return getRemote(req, resp, repo, path, true, Collections.EMPTY_SET, file);
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
	protected synchronized final void doPut(final HttpServletRequest req, final HttpServletResponse resp)
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
			configManager.getLogger().log(Level.FINE, "create directory {0}", file);
		}

		OutputStream fout = null;
		try {
			fout = new FileOutputStream(file);
			Io.copy(req.getInputStream(), fout);
		} finally {
			Io.close(fout);
		}
		if (file.getName().equals("maven-metadata.xml") && parent.getName().endsWith("-SNAPSHOT")) {
			CleanUp.cleanUpSnapshots(file, configManager, repo, this);
		}
	}

	@Override
	protected String getTitle() {
		return null;
	}

	@Override
	protected void doContent(HttpServletRequest req, Writer writer) throws ServletException, IOException {
		writer.write("<table cellspacing=\"10\"><tr><th align=\"left\">"
				+ "Name</th><th>Last Modified</th><th>Size</th></tr>");

		ListCallback cb = (ListCallback) req.getAttribute("cb");
		cb.fillTable(writer);

		writer.write("</table>");
		
	}
}
