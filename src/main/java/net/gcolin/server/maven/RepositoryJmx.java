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
 * Repository JMX API.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public interface RepositoryJmx {

    /**
     * Set the repository name. WARNING: The directory that contains files is
     * not renamed.
     *
     * @param name the new repository name
     */
    void rename(String name);

    /**
     * Update the remote URL of a repository.
     *
     * @param url the remote URL
     */
    void updateRemoteUrl(String url);

    /**
     * Include a repository to this one.
     *
     * @param name The name of the repository to include
     */
    void addInclude(String name);

    /**
     * Exclude a repository from this one.
     *
     * @param name The name of the repository to exclude
     */
    void removeInclude(String name);

    /**
     * Remove this repository and remove the references in other repositories.
     * WARNING: The files are not removed.
     */
    void remove();

    /**
     * Get the included repositories.
     *
     * @return the names of the included repositories.
     */
    String[] getIncludes();

    /**
     * Get the name of the repository.
     *
     * @return The name of the repository
     */
    String getName();

    /**
     * Get the remote URL of the repository.
     *
     * @return The remote URL of the repository
     */
    String getRemoteUrl();

}
