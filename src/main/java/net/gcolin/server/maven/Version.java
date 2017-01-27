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

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * SnapshotVersion model in maven-metadata.xml.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
@XmlRootElement(name = "snapshotVersion")
public class Version {

    /**
     * The classifier.
     */
    private String classifier;
    /**
     * The extension.
     */
    private String extension;
    /**
     * The value.
     */
    private String value;
    /**
     * The updated date.
     */
    private String updated;
    /**
     * The associated files.
     */
    private transient List<VersionFile> matches = new ArrayList<VersionFile>();

    /**
     * @return the classifier
     */
    public final String getClassifier() {
        return classifier;
    }

    /**
     * @param newclassifier the classifier to set
     */
    public final void setClassifier(final String newclassifier) {
        this.classifier = newclassifier;
    }

    /**
     * @return the extension
     */
    public final String getExtension() {
        return extension;
    }

    /**
     * Set the extension.
     *
     * @param newextension the extension to set
     */
    public final void setExtension(final String newextension) {
        this.extension = newextension;
    }

    /**
     * @return the value
     */
    public final String getValue() {
        return value;
    }

    /**
     * @param val the value to set
     */
    public final void setValue(final String val) {
        this.value = val;
    }

    /**
     * @return the updated
     */
    public final String getUpdated() {
        return updated;
    }

    /**
     * @param newupdated the updated to set
     */
    public final void setUpdated(final String newupdated) {
        this.updated = newupdated;
    }

    /**
     * @return the matches
     */
    public final List<VersionFile> getMatches() {
        return matches;
    }

    /**
     * @param newmatches the matches to set
     */
    public final void setMatches(final List<VersionFile> newmatches) {
        this.matches = newmatches;
    }

}
