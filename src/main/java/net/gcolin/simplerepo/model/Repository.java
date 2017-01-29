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

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Repository model.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository {

    /**
     * The repository name.
     */
    private String name;
    /**
     * The remote URL.
     */
    private String remote;
    /**
     * The included repositories.
     */
    private List<String> includes;
    /**
     * the max-age before checking.
     */
    private long artifactMaxAge = -1L;

    /**
     * Get name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Set name.
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get remote URL.
     *
     * @return remote URL
     */
    public String getRemote() {
        return remote;
    }

    /**
     * Set remote URL.
     *
     * @param remote remote URL
     */
    public void setRemote(String remote) {
        this.remote = remote;
    }

    /**
     * Get included repositories.
     *
     * @return included repositories
     */
    public List<String> getIncludes() {
        return includes;
    }

    /**
     * Set included repositories.
     *
     * @param includes included repositories
     */
    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    /**
     * @return the artifactMaxAge
     */
    public long getArtifactMaxAge() {
        return artifactMaxAge;
    }

    /**
     * @param artifactMaxAge the artifactMaxAge to set
     */
    public void setArtifactMaxAge(long artifactMaxAge) {
        this.artifactMaxAge = artifactMaxAge;
    }

}
