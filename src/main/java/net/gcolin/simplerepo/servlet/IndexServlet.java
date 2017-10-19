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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.gcolin.simplerepo.business.SearchManager;
import net.gcolin.simplerepo.model.Result;

/**
 * Display index.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class IndexServlet extends HttpServlet {

    private static final long serialVersionUID = -6005367190609545871L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String groupId = req.getParameter("groupId");
        String artifactId = req.getParameter("artifactId");
        String version = req.getParameter("version");
        String q = req.getParameter("q");
        
        req.setAttribute("q", q);
        Result result = null;
        if (version != null && artifactId != null && groupId != null) {
            SearchManager search = (SearchManager) getServletContext().getAttribute("searchManager");
            req.setAttribute("result", result = search.get(groupId, artifactId, version));
        } else if (q != null || groupId != null) {
            SearchManager search = (SearchManager) getServletContext().getAttribute("searchManager");
            String offset = req.getParameter("offset");
            req.setAttribute("result", result = search.search(groupId, artifactId, q, offset == null || offset.isEmpty() ? 0 : Integer.parseInt(offset)));
        }
        if(result != null) {
          StringBuilder str = new StringBuilder();
          str.append(req.getContextPath());
          if(str.length() > 1) {
            str.append("/?");
          }
          if(q != null) {
            str.append("q=").append(Util.encode(q)).append('&');
          }
          if(groupId != null) {
            str.append("groupId=").append(Util.encode(groupId)).append('&');
          }
          if(artifactId != null) {
            str.append("artifactId=").append(Util.encode(artifactId)).append('&');
          }
          if(version != null) {
            str.append("version=").append(Util.encode(version)).append('&');
          }
          str.append("offset=");
          result.setBase(str.toString());
        }
        req.getServletContext().getRequestDispatcher("/WEB-INF/index.ftl").forward(req, resp);
    }

}
