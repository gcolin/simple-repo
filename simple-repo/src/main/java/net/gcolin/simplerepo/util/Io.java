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
package net.gcolin.simplerepo.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Operations about IO.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class Io {

  private Io() {}

  /**
   * Uncompress a compressed file
   * 
   * @param zipfile the input file
   * @param extractFolder the output directory
   * @throws IOException if an I/O error occurs.
   */
  public static void unzip(File zipfile, File extractFolder) throws IOException {
    ZipFile zip = null;
    try {
      zip = new ZipFile(zipfile);
      Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
      while (zipFileEntries.hasMoreElements()) {
        ZipEntry entry = zipFileEntries.nextElement();
        String currentEntry = entry.getName();
        File destFile = new File(extractFolder, currentEntry);
        if (!entry.isDirectory()) {
          unzipEntry(zip, entry, destFile);
        }
      }
    } finally {
      close(zip);
    }
  }

  private static void unzipEntry(ZipFile zip, ZipEntry entry, File destFile) throws IOException {
    OutputStream out = null;
    InputStream in = null;
    try {
      destFile.getParentFile().mkdirs();
      out = new FileOutputStream(destFile);
      in = zip.getInputStream(entry);
      copy(in, out);
    } finally {
      close(out);
      close(in);
    }
  }

  /**
   * Copy a stream to another.
   * 
   * @param in an input stream
   * @param out an output stream
   * @throws IOException if an I/O error occurs.
   */
  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[1024];
    int count = -1;
    while ((count = in.read(buf)) != -1) {
      out.write(buf, 0, count);
    }
  }

  /**
   * Close a stream.
   * 
   * @param cl a stream
   */
  public static void close(Closeable cl) {
    if (cl != null) {
      try {
        cl.close();
      } catch (IOException ex) {
        Logger.getLogger(Io.class.getName()).log(Level.FINE, ex.getMessage(), ex);
      }
    }
  }
}
