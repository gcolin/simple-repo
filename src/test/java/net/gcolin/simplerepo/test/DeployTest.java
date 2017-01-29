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
package net.gcolin.simplerepo.test;

import java.io.File;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test deploy a file.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class DeployTest extends AbstractRepoTest {

    @Test
    public void test() throws Exception {
        Server server = createServer(18080, "server");
        try {
            addRepository("server", "test", null);

            File file = new File("target/reposerver/test/foo/bar.txt");

            Assert.assertEquals(401, sendContent("http://localhost:18080/simple-repo/repository/test/foo/bar.txt", "hello", null, null));
            Assert.assertFalse(file.exists());

            Assert.assertEquals(200, sendContent("http://localhost:18080/simple-repo/repository/test/foo/bar.txt", "hello", "user", "user"));
            Assert.assertTrue(file.exists());

            Assert.assertEquals("hello", getContent("http://localhost:18080/simple-repo/repository/test/foo/bar.txt", 0));
        } finally {
            server.stop();
        }
    }

}
