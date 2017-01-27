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

import java.io.File;
import java.io.Serializable;
import java.util.Comparator;

/**
 * Compare file by name.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class FileNameComparator implements Comparator<File>, Serializable {

    /**
     * A unique serial version identifier.
     *
     * @see Serializable#serialVersionUID
     */
    private static final long serialVersionUID = 479949221935034738L;

    @Override
    public final int compare(final File f1, final File f2) {
        if (f1.isDirectory() && f2.isDirectory()) {
            return f1.getName().compareToIgnoreCase(f2.getName());
        }
        if (f1.isDirectory()) {
            return -1;
        }
        if (f2.isDirectory()) {
            return 1;
        }
        return f1.getName().compareToIgnoreCase(f2.getName());
    }

}
