/*
 * Copyright 2017 Admin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.gcolin.server.maven.plugin.search;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 *
 * @author Admin
 */
@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(ResultType.class)
public class SearchResult {

    private String groupId;
    private String artifactId;
    private String version;
    private String repoName;
    private Long lastUpdate;
    private Long id;
    private List<ResultType> types = new ArrayList<ResultType>();

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
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

    /**
     * @return the lastUpdate
     */
    public Long getLastUpdate() {
        return lastUpdate;
    }

    /**
     * @param lastUpdate the lastUpdate to set
     */
    public void setLastUpdate(Long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * @return the types
     */
    public List<ResultType> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    public void setTypes(List<ResultType> types) {
        this.types = types;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the repoName
     */
    public String getRepoName() {
        return repoName;
    }

    /**
     * @param repoName the repoName to set
     */
    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

}
