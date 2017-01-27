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

/**
 * VersionFile model in maven-metadata.xml.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class VersionFile implements Comparable<VersionFile> {

    /**
     * The file name.
     */
    private String file;
    /**
     * The file version.
     */
    private String version;

    /**
     * @return the file
     */
    public final String getFile() {
        return file;
    }

    /**
     * @param newfile the file to set
     */
    public final void setFile(final String newfile) {
        this.file = newfile;
    }

    /**
     * @return the version
     */
    public final String getVersion() {
        return version;
    }

    /**
     * @param ver the version to set
     */
    public final void setVersion(final String ver) {
        this.version = ver;
    }

    @Override
    public final int compareTo(final VersionFile o) {
        return getVersion().compareTo(o.getVersion());
    }

    @Override
    public final boolean equals(final Object obj) {
        return obj != null
                && obj instanceof VersionFile
                && (getVersion() == null
                && ((VersionFile) obj).getVersion() == null
                || getVersion().equals(((VersionFile) obj).getVersion()));
    }

    @Override
    public final int hashCode() {
        if (version == null) {
            return 0;
        } else {
            return this.version.hashCode();
        }
    }

}
