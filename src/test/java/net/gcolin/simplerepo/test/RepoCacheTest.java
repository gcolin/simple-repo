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
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cache.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class RepoCacheTest extends AbstractRepoTest {

    @Test
    public void test() throws Exception {
        Server server1 = createServer(18080, "server1");
        Server server2 = createServer(18081, "server2");
        try {
            addRepository("server1", "test", null);
            addRepository("server2", "test", "http://localhost:18080/simple-repo/repository/test/");

            File file = new File("target/reposerver1/test/foo/bar.txt");
            file.getParentFile().mkdirs();
            FileUtils.write(file, "hello", "utf-8");
            Assert.assertEquals("hello", getContent("http://localhost:18081/simple-repo/repository/test/foo/bar.txt", 0));
            Thread.sleep(1000);
            FileUtils.write(file, "world", "utf-8");
            Assert.assertEquals("hello", getContent("http://localhost:18081/simple-repo/repository/test/foo/bar.txt", 0));

            setArtifactMaxAge("server2", "test", 100);
            Thread.sleep(1000);
            Assert.assertEquals("world", getContent("http://localhost:18081/simple-repo/repository/test/foo/bar.txt", 0));
            File file2 = new File("target/reposerver2/test/foo/bar.txt");
            long lastUpdate = file2.lastModified();
            Thread.sleep(1000);
            Assert.assertEquals("world", getContent("http://localhost:18081/simple-repo/repository/test/foo/bar.txt", 0));
            Assert.assertTrue(lastUpdate < file2.lastModified());

            lastUpdate = file2.lastModified();
            FileUtils.write(file2, "world2", "utf-8");
            file2.setLastModified(lastUpdate);
            Thread.sleep(1000);
            Assert.assertEquals("world2", getContent("http://localhost:18081/simple-repo/repository/test/foo/bar.txt", lastUpdate));
        } finally {
            server1.stop();
            server2.stop();
        }
    }

}
