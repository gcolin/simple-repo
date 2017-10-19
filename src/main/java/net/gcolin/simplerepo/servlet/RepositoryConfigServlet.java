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

import net.gcolin.simplerepo.model.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.gcolin.simplerepo.business.ConfigurationManager;

/**
 * A servlet for configuring the repositories.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepositoryConfigServlet extends HttpServlet {

    private static final long serialVersionUID = 8125626220419384344L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String reponame = (String) req.getAttribute("r");
        if (reponame == null) {
            reponame = req.getParameter("r");
        }
        req.setAttribute("r", reponame);
        req.setAttribute("a", req.getParameter("a"));
        boolean isNew = reponame == null && "new".equals(req.getParameter("a"));
        req.setAttribute("isNew", isNew);
        
        ConfigurationManager configManager = (ConfigurationManager) getServletContext().getAttribute("configManager");
        
        Repository selected = null;
        for (Repository repo : configManager.getRepositories()) {
            if (repo.getName().equals(reponame)) {
                selected = repo;
            }
        }
        
        if (isNew) {
            selected = new Repository();
        }
        req.setAttribute("selected", selected);
        getServletContext().getRequestDispatcher("/WEB-INF/repository.ftl").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String[] actions = req.getParameterValues("a");

        String action;
        if (actions == null) {
            action = null;
        } else if (actions.length == 1) {
            action = actions[0];
        } else {
            List<String> list = Arrays.asList(actions);
            if (list.contains("del")) {
                action = "del";
            } else {
                action = list.get(0);
            }
        }

        ConfigurationManager configManager
                = (ConfigurationManager) getServletContext().getAttribute("configManager");

        if ("global".equals(action)) {
            try {
                int maxsnapshots = Integer.parseInt(req.getParameter("maxsnapshots"));
                long notfoundcache = Long.parseLong(req.getParameter("notfoundcache"));
                if (maxsnapshots != configManager.getMaxSnapshots()) {
                    configManager.setMaxSnapshots(maxsnapshots);
                }
                if (notfoundcache != configManager.getNotFoundCache()) {
                    configManager.setNotFoundCache(maxsnapshots);
                }
                configManager.setTheme(req.getParameter("theme"));
            } catch (NumberFormatException ex) {
                req.setAttribute("error", "Bad number format " + ex.getMessage());
                doGet(req, resp);
                return;
            }
            req.setAttribute("success", "Saved");
            doGet(req, resp);
            return;
        }

        String reponame = req.getParameter("r");
        String name = req.getParameter("name");
        Repository repo = null;

        if (reponame == null && (name == null || name.isEmpty())) {
            req.setAttribute("error", "The new repository must have a name");
            doGet(req, resp);
            return;
        }

        if (reponame != null) {
            repo = configManager.getRepository(reponame);
            if (repo == null) {
                req.setAttribute("error", "The repository does not exists");
                doGet(req, resp);
                return;
            }
        }

        String[] includes = req.getParameterValues("included");
        if (includes == null) {
            includes = new String[0];
        }
        String remote = req.getParameter("remote");

        if (remote != null && !remote.isEmpty() && includes != null && includes.length > 0) {
            req.setAttribute("error",
                    "The repository cannot have a remote URL and include other repositories");
            doGet(req, resp);
            return;
        }

        ConfigurationManager.RepoHandle handle = configManager.toHandle(repo);
        try {
            if ("del".equals(action)) {
                handle.remove();
                doGet(req, resp);
                return;
            }

            if (reponame == null) {
                configManager.newRepository(name);
                repo = configManager.getRepository(name);
                handle = configManager.toHandle(repo);
            } else if (!Objects.equals(repo.getName(), name)) {
                handle.rename(name);
            }
            req.setAttribute("r", repo.getName());
            if (!Objects.equals(repo.getRemote(), remote)) {
                handle.updateRemoteUrl(remote);
            }
            List<String> repoIncludes = new ArrayList<>();
            if (repo.getIncludes() != null) {
                repoIncludes.addAll(repo.getIncludes());
            }
            for (String include : includes) {
                if (repoIncludes.contains(include)) {
                    repoIncludes.remove(include);
                } else {
                    handle.addInclude(include);
                }
            }
            for (String include : repoIncludes) {
                handle.removeInclude(include);
            }
            req.setAttribute("success", "Saved");
            doGet(req, resp);
        } catch (IllegalArgumentException ex) {
            req.setAttribute("error", ex.getMessage());
            doGet(req, resp);
        }

    }

}
