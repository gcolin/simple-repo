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
package net.gcolin.simplerepo.util;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * List all files in directory (virtual or not).
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class DirectoryListCallback extends ListCallback {

    /**
     * All files to display.
     */
    private final List<File> children;

    /**
     * Create DirectoryListCallback.
     *
     * @param newchildren children
     */
    public DirectoryListCallback(List<File> newchildren) {
        this.children = newchildren;
    }

    @Override
    public void fillTable(Writer writer) throws IOException {
        writer.write("<tr><td><a href=\"../\">Parent Directory</a>"
                + "</td><td></td><td></td></tr>");
        Collections.sort(children, new FileNameComparator());
        Set<String> displayed = new HashSet<String>();
        for (File child : children) {
            if (displayed.contains(child.getName())) {
                continue;
            }
            displayed.add(child.getName());
            writer.write("<tr><td><a href=\"");
            writer.write(child.getName());
            if (child.isDirectory()) {
                writer.write("/");
            }
            writer.write("\">");
            writer.write(child.getName());
            if (child.isDirectory()) {
                writer.write("/");
            }
            writer.write("</a></td>");
            writer.write("<td>");
            writer.write(formatDate(child.lastModified()));
            writer.write("</td><td>");
            if (child.exists() && child.isFile()) {
                writer.write(Long.toString(child.length()));
            }
            writer.write("</td></tr>");
        }
    }

}
