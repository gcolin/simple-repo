/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
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

package net.gcolin.server.maven.plugin.search;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Redirect Derby logs to JUL.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class DerbyToLogOutputStream extends OutputStream {

  private byte[] lineSeparator;
  private byte[] buffer = new byte[1024];
  private int bufferSize = 0;
  private int sepIndex = 0;
  private Logger logger = Logger.getLogger("derby");

  /**
   * Create a DerbyToLogOutputStream.
   */
  public DerbyToLogOutputStream() {
    StringWriter sw = new StringWriter();
    BufferedWriter buf = new BufferedWriter(sw, 2);
    try {
      buf.newLine();
      buf.close();
      lineSeparator = sw.toString().getBytes(StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void write(int buff) throws IOException {
    byte bt = (byte) buff;
    if (bt == lineSeparator[sepIndex]) {
      sepIndex++;
      if (sepIndex == lineSeparator.length) {
        logger.info(new String(buffer, 0, bufferSize, StandardCharsets.UTF_8));
        bufferSize = 0;
        sepIndex = 0;
      }
    } else {
      if (sepIndex > 0) {
        sepIndex = 0;
      }
      buffer[bufferSize++] = (byte) buff;
      if (bufferSize == buffer.length) {
        logger.info(new String(buffer, 0, bufferSize, StandardCharsets.UTF_8));
        bufferSize = 0;
      }
    }

  }

}
