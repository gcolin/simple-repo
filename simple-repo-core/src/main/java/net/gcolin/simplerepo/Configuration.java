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

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Configuration model.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
@XmlRootElement(name = "config")
@XmlSeeAlso(Repository.class)
public class Configuration {

    /**
     * All repositories.
     */
    private List<Repository> repositories;

    /**
     * Maximum number of snapshots stored.
     */
    private int maxSnapshots = 10;
    
    /**
     * Time before the repository retry to get a not found file.
     */
    private long notFoundCache = TimeUnit.DAYS.toMicros(1L);

    /**
     * Get repositories.
     *
     * @return repositories.
     */
    public final List<Repository> getRepositories() {
        return repositories;
    }

    /**
     * Set repositories.
     *
     * @param repo repositories
     */
    public final void setRepositories(final List<Repository> repo) {
        this.repositories = repo;
    }

    /**
     * Get max snapshots.
     *
     * @return max snapshots
     */
    public final int getMaxSnapshots() {
        return maxSnapshots;
    }

    /**
     * Set max snapshots.
     *
     * @param max max snapshots
     */
    public final void setMaxSnapshots(final int max) {
        this.maxSnapshots = max;
    }

    /**
     * @return the notFoundCache
     */
    public long getNotFoundCache() {
        return notFoundCache;
    }

    /**
     * @param notFoundCache the notFoundCache to set
     */
    public void setNotFoundCache(long notFoundCache) {
        this.notFoundCache = notFoundCache;
    }

}
