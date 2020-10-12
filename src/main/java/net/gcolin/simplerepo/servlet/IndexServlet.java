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

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Display index.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class IndexServlet extends GenericServlet {

  private static final long serialVersionUID = 2586253416946691092L;

  @Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
	  HttpServletResponse resp = (HttpServletResponse) res;
	  resp.sendRedirect("repository");
	  resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
	}

}