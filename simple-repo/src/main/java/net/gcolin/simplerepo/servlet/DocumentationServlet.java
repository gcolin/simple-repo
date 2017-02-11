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
package net.gcolin.simplerepo.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * A servlet for rendering the documentation.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class DocumentationServlet extends AbstractDisplayServlet {

  private static final long serialVersionUID = -2535139795815093719L;

  @Override
  protected String getTitle() {
    return "Simple repo - Documentation";
  }

  @Override
  protected void doContent(HttpServletRequest req, Writer writer)
      throws ServletException, IOException {
    InputStream in = getServletContext().getResourceAsStream("docs.html");
    if(in != null) {
      try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)){
        char[] cbuf = new char[100];
        int count;
        while((count = reader.read(cbuf)) != -1) {
          writer.write(cbuf, 0, count);
        }
      } finally {
        in.close();
      }
    }
  }

}
