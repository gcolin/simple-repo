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

import java.io.File;
import java.util.EventListener;
import net.gcolin.simplerepo.model.Repository;

/**
 * Listen Repository updates.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public interface RepositoryListener extends EventListener {
    
    /**
     * For further use. Indexing for example
     *
     * @param file the file
     * @param repository the repository
     * @param remote the file was retrieve from a remote location
     * @param override the file was override
     */
    void onRecieveFile(File file, Repository repository, boolean remote, boolean override);
    
    /**
     * For further use. Indexing for example
     *
     * @param file the file
     * @param repository the repository
     * @param remote the file was retrieve from a remote location
     */
    void onRemoveFile(File file, Repository repository, boolean remote);
    
}
