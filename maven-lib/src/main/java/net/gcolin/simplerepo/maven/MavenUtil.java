/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.maven;

import net.gcolin.simplerepo.model.ResultType;
import net.gcolin.simplerepo.model.SearchResult;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Some Maven reusable methods.
 *
 * @author Gaël COLIN
 * @since 1.0
 */
public class MavenUtil {

  public Model detectModel(File file) throws IOException {
    if (!file.getName().endsWith(".pom")) {
      File[] children = file.getParentFile().listFiles(new FilenameFilter() {

        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".pom");
        }
      });
      if (children != null && children.length > 0) {
        return detectModel(children[0]);
      } else {
        return null;
      }
    } else {
      return readPom(file);
    }

  }
  
  public ResultType getType(Model model, String fileName) {
    if (!fileName.endsWith(".md5") && !fileName.endsWith(".sha1") && !fileName.endsWith(".notfound")
        && !fileName.endsWith(".pom")) {
      String base = model.getArtifactId() + "-" + model.getVersion();
      if (!fileName.startsWith(base)) {
        return null;
      }
      String end = fileName.substring(base.length() + 1);
      int split = end.indexOf('.');
      ResultType type = new ResultType();
      if (split == -1) {
        type.setName(end);
      } else {
        type.setName(end.substring(split + 1));
        type.setClassifier(end.substring(0, split));
      }
      return type;
    }
    return null;
  }
  
  public Model readPom(File file) throws IOException {
    Model model;
    try (InputStream in = new FileInputStream(file)) {
      model = new MavenXpp3Reader().read(in);
      model.setPomFile(file);
    } catch (XmlPullParserException ex) {
      throw new IOException(ex);
    }

    if (model.getGroupId() == null && model.getParent() != null) {
      model.setGroupId(model.getParent().getGroupId());
    }

    if (model.getArtifactId() == null && model.getParent() != null) {
      model.setArtifactId(model.getParent().getArtifactId());
    }

    if (model.getVersion() == null && model.getParent() != null) {
      model.setVersion(model.getParent().getVersion());
    }

    if (model.getName() == null) {
      model.setName(model.getArtifactId());
    }
    return model;
  }
  
  public SearchResult buildResult(final String repoName, File pomFile, Model model)
      throws IOException {
    SearchResult result = new SearchResult();
    result.setRepoName(repoName);
    result.setGroupId(model.getGroupId());
    result.setArtifactId(model.getArtifactId());
    result.setVersion(model.getVersion());
    ResultType pom = new ResultType();
    pom.setName("pom");
    result.getTypes().add(pom);
    File[] children = pomFile.getParentFile().listFiles();
    if (children != null) {
      for (File child : children) {
        String cname = child.getName();
        ResultType type = getType(model, cname);
        if (type != null) {
          result.getTypes().add(type);
        }
      }
    }
    return result;
  }
  
}
