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
    public String getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(String file) {
        this.file = file;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public int compareTo(VersionFile o) {
        return getVersion().compareTo(o.getVersion());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null
                && obj instanceof VersionFile
                && (getVersion() == null
                && ((VersionFile) obj).getVersion() == null
                || getVersion().equals(((VersionFile) obj).getVersion()));
    }

    @Override
    public int hashCode() {
        if (version == null) {
            return 0;
        } else {
            return this.version.hashCode();
        }
    }

}
