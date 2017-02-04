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

import java.io.IOException;
import java.io.Writer;
import java.util.EventListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Listener when index page is displayed.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public interface IndexListener extends EventListener {

    void onHead(HttpServletRequest req, Writer writer)
            throws IOException, ServletException;
    
    void onStartBody(HttpServletRequest req, Writer writer)
            throws IOException, ServletException;
    
    void onEndBody(HttpServletRequest req, Writer writer)
            throws IOException, ServletException;
    
    void onIndex(HttpServletRequest req, Writer writer)
            throws IOException, ServletException;

}
