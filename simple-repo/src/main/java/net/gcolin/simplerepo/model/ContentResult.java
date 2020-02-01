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
package net.gcolin.simplerepo.model;

import java.io.File;
import java.util.List;

/**
 * Contains the data to send.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class ContentResult {

    /**
     * A file to send.
     */
    private File file;
    /**
     * All files to send in an HTML.
     */
    private List<File> children;

    /**
     * Get a file to send.
     *
     * @return a file.
     */
    public final File getFile() {
        return file;
    }

    /**
     * Set a file to send.
     *
     * @param newfile file
     */
    public final void setFile(final File newfile) {
        this.file = newfile;
    }

    /**
     * Get all files to send in an HTML.
     *
     * @return children of the requested file.
     */
    public final List<File> getChildren() {
        return children;
    }

    /**
     * Set all files to send in an HTML.
     *
     * @param newchildren children of the requested file
     */
    public final void setChildren(final List<File> newchildren) {
        this.children = newchildren;
    }

    /**
     * Check if the response is empty.
     *
     * @return true if there is nothing to send
     */
    public final boolean isEmpty() {
        return file == null && children == null;
    }

}
