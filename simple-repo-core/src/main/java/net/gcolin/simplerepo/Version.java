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
package net.gcolin.simplerepo;

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
    public String getClassifier() {
        return classifier;
    }

    /**
     * @param classifier the classifier to set
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * @return the extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Set the extension.
     *
     * @param extension the extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the updated
     */
    public String getUpdated() {
        return updated;
    }

    /**
     * @param updated the updated to set
     */
    public void setUpdated(String updated) {
        this.updated = updated;
    }

    /**
     * @return the matches
     */
    public List<VersionFile> getMatches() {
        return matches;
    }

    /**
     * @param matches the matches to set
     */
    public void setMatches(List<VersionFile> matches) {
        this.matches = matches;
    }

}
