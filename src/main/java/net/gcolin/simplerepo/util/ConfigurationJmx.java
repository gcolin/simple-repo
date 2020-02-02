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
package net.gcolin.simplerepo.util;

/**
 * Configuration JMX API.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public interface ConfigurationJmx {

    /**
     * Create a new repository.
     *
     * @param name repository name
     */
    void newRepository(String name);

    /**
     * Get the max snapshots value.
     *
     * @return the max snapshots value
     */
    int getMaxSnapshots();

    /**
     * Set the max snapshots value.
     *
     * @param maxSnapshots the max snapshots value
     */
    void setMaxSnapshots(int maxSnapshots);
    
    /**
     * Get the not found cache.
     *
     * @return the not found cache
     */
    long getNotFoundCache();

    /**
     * Set the not found cache.
     *
     * @param notfoundcache the not found cache
     */
    void setNotFoundCache(long notfoundcache);

}
