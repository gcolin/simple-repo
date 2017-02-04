/*
 * Copyright 2017 Admin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.servlet;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import javax.servlet.http.HttpServlet;

/**
 *
 * @author Admin
 */
public abstract class AbstractIoServlet extends HttpServlet {

  private static final long serialVersionUID = 7581279604138518430L;

  /**
   * Copy a stream in another.
   *
   * @param in the input stream
   * @param out the output stream
   * @throws IOException if an error occurs
   */
  protected void copy(final InputStream in, final OutputStream out) throws IOException {
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
  protected void close(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ex) {
        StartListener.LOG.log(Level.FINE, null, ex);
      }
    }
  }

}
