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

import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import java.io.IOException;
import java.net.HttpURLConnection;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import javax.servlet.GenericServlet;
import net.gcolin.simplerepo.business.ConfigurationManager;
import net.gcolin.simplerepo.model.FreeMarkerModel;

public class FreeMarkerServlet extends GenericServlet {

    private static final long serialVersionUID = -8464854558466464983L;

    private Configuration configuration;
    private ConfigurationManager configManager;

    @Override
    public void init() throws ServletException {
        super.init();
        configManager = (ConfigurationManager) getServletContext().getAttribute("configManager");
        configuration = new Configuration();
        configuration.setTemplateLoader(new WebappTemplateLoader(getServletContext()) {
            @Override
            public long getLastModified(Object templateSource) {
                return 1;
            }
        });
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setCacheStorage(new freemarker.cache.MruCacheStorage(100, 500));
        getServletContext().setAttribute("freemarkerConfiguration", configuration);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String servletPath = null;
        String pathInfo = null;

        if (request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI) != null) {
            servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
            pathInfo = (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
        }
        if (servletPath == null) {
            servletPath = request.getServletPath();
            pathInfo = request.getPathInfo();
        }

        String path;
        if (servletPath.length() == 0) {
            path = pathInfo;
        } else if (pathInfo == null) {
            path = servletPath;
        } else {
            path = servletPath + pathInfo;
        }

        Template template = configuration.getTemplate(path);

        if (template == null) {
            response.sendError(HttpURLConnection.HTTP_NOT_FOUND);
            return;
        }

        try {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html");
            FreeMarkerModel model = new FreeMarkerModel(request, configManager);
            template.process(model, response.getWriter());
        } catch (TemplateException ex) {
            throw new ServletException(ex);
        }

    }

}
