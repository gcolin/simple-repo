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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Display index.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public abstract class AbstractDisplayServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected abstract String getTitle();

	protected abstract void doContent(HttpServletRequest req, Writer writer) throws ServletException, IOException;

	@Override
	protected final void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding("utf-8");
		resp.setContentType("text/html");
		Writer writer = resp.getWriter();
		writer.append("<html><head><title>");
		writer.append(getTitle());
		writer.append("</title>" + "<meta charset=\"UTF-8\"><meta name=\"viewport\" "
				+ "content=\"width=device-width, initial-scale=1.0\">");
		writer.append("<link href=\"");
		String contextName = req.getServletContext().getServletContextName();
		if (!"/".equals(contextName)) {
			writer.append("/");
			writer.append(contextName);
		}
		writer.append("/entireframework.min.css\" rel=\"stylesheet\" type=\"text/css\">");

		req.setAttribute("title", getTitle());
		doMenu(req);

		writer.append("</head><body>");

		writeMenu(req, writer);

		writer.append("<div class=\"container\">");
		
		if (req.getAttribute("notitle") == null) {
			writer.append("<h1>");
			writer.append(getTitle());
			writer.append("</h1>");
		}
		doContent(req, writer);
		writer.append("</div>");
	}

	protected void doMenu(HttpServletRequest req) {
		String base = req.getContextPath();
		if (base.equals("/")) {
			base = "";
		}
		Map<String, String> menu = new LinkedHashMap<>();
		menu.put(base + "/repository/", "All repositories");
		menu.put(base + "/config/repository", "Repositories");
		req.setAttribute("menu", menu);
	}

	@SuppressWarnings("unchecked")
	protected void writeMenu(HttpServletRequest req, Writer writer) throws IOException {
		writer.append("<nav class=\"nav\"><div class=\"container\"><a class=\"pagename current\" href=\"#\">Your Site Name</a>");
		for (Entry<String, String> entry : ((Map<String, String>) req.getAttribute("menu")).entrySet()) {
			writer.append("<a href=\"");
			writer.append(entry.getKey());
			writer.append("\">");
			writer.append(entry.getValue());
			writer.append("</a>");
		}
		writer.append("</div></nav>");
	}

}
