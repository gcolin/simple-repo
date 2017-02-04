/*
 * Copyright 2017 Admin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.gcolin.maven.server.plugin.bootstrap;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import net.gcolin.server.maven.IndexListener;

/**
 *
 * @author Admin
 */
public class BootstrapListener implements ServletContextListener, IndexListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    @Override
    public void onHead(HttpServletRequest req, Writer writer) throws IOException, ServletException {
        writer.append("<link href=\"resources/bootstrap/css/bootstrap.min.css\" rel=\"stylesheet\">");
        writer.append("<link href=\"resources/bootstrap/css/bootstrap-theme.min.css\" rel=\"stylesheet\">");
        writer.append("<script type='text/javascript' src=\"resources/jquery/jquery-3.1.0.min.js\"></script>");
        writer.append("<script type='text/javascript' src=\"resources/bootstrap/js/bootstrap.min.js\"></script>");
        req.setAttribute("nomenu", true);
        req.setAttribute("notitle", true);
    }

    @Override
    public void onStartBody(HttpServletRequest req, Writer writer) throws IOException, ServletException {
        writer.append("<nav class=\"navbar navbar-inverse\">");
        writer.append("<div class=\"container\">");
        writer.append("<div class=\"navbar-header\">");
        writer.append("<button type=\"button\" class=\"navbar-toggle collapsed\" data-toggle=\"collapse\" data-target=\"#navbar\" aria-expanded=\"false\" aria-controls=\"navbar\">");
        writer.append("<span class=\"sr-only\">Toggle navigation</span>");
        writer.append("<span class=\"icon-bar\"></span>");
        writer.append("<span class=\"icon-bar\"></span>");
        writer.append("<span class=\"icon-bar\"></span>");
        writer.append("</button><a class=\"navbar-brand\" href=\"#\">Simple repo</a></div>");
        writer.append("<div id=\"navbar\" class=\"collapse navbar-collapse\">");
        writer.append("<ul class=\"nav navbar-nav\">");
        writer.append("<li><a href=\"repository/\">All repositories</a></li>");
        writer.append("<li><a href=\"docs.html\">Documentation</a></li>");
        writer.append("</ul></div></div></nav>");
        writer.append("<div class=\"container\">");
    }

    @Override
    public void onEndBody(HttpServletRequest req, Writer writer) throws IOException, ServletException {
        writer.append("</div>");
    }

    @Override
    public void onIndex(HttpServletRequest req, Writer writer) throws IOException, ServletException {
    }
    
}
