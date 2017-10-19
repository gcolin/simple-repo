/*
 * Copyright 2017 gcolin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package net.gcolin.simplerepo.business;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.gcolin.simplerepo.model.Result;
import net.gcolin.simplerepo.model.ResultType;
import net.gcolin.simplerepo.model.SearchModel;
import net.gcolin.simplerepo.model.SearchResult;

/**
 *
 * @author gcolin
 */
public class SearchManager implements Closeable {


  private ServletContext context;

  public SearchManager(ServletContext context) {
    this.context = context;
  }

  @Override
  public void close() throws IOException {}

  private Result process(String query, String groupId, String artifactId, String version,
      int offset) {
    Result result = new Result();
    result.setArtifactId(artifactId);
    result.setGroupId(groupId);
    result.setVersion(version);
    result.setOffset(offset);

    try {
      SearchModel search = new SearchModel(groupId, artifactId, version, query, offset, 20);

      Configuration config = (Configuration) context.getAttribute("freemarkerConfiguration");
      Template template = config.getTemplate("/WEB-INF/search/get.ftl");
      StringWriter sw = new StringWriter();
      template.process(search, sw);

      ConfigurationManager configManager =
          (ConfigurationManager) context.getAttribute("configManager");

      URL url = new URL(configManager.getElasticsearch() + "/maven/_search");
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      JSONObject obj = null;
      try {
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.getOutputStream().write(sw.toString().getBytes(StandardCharsets.UTF_8));
        conn.getOutputStream().flush();
        obj = new JSONObject(IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));
      } finally {
        IOUtils.close(conn);
      }

      JSONObject hits = obj.getJSONObject("hits");
      result.setCount(hits.getInt("total"));
      JSONArray array = hits.getJSONArray("hits");
      for (int i = 0; i < array.length(); i++) {
        JSONObject o = array.getJSONObject(i);
        SearchResult res = new SearchResult();
        res.setArtifactId(o.getString("artifactId"));
        res.setGroupId(o.getString("groupÌd"));
        res.setVersion(o.getString("version"));
        res.setLastUpdate(o.getLong("lastUpdate"));
        res.setTypes(o.getJSONArray("types").toList().stream().map(x -> {
          ResultType type = new ResultType();
          type.setClassifier(((JSONString) x).toJSONString());
          return type;
        }).collect(Collectors.toList()));
      }

    } catch (TemplateException | IOException ex) {
      throw new RuntimeException(ex);
    }

    return result;
  }

  public Result get(String groupId, String artifactId, String version) {
    return process(null, groupId, artifactId, version, 0);
  }

  public Result search(String groupId, String artifactId, String q, int offset) {
    return process(q, groupId, artifactId, null, 0);
  }
}
