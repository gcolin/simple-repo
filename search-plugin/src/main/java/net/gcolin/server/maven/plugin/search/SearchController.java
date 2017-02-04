/*
 * Copyright 2017 Admin.
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
package net.gcolin.server.maven.plugin.search;

import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.util.ConfigurationManager;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 *
 * @author Admin
 */
public class SearchController implements SearchControllerJmx {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private DataSource datasource;
  private ConfigurationManager configManager;
  private final ResultSetHandler<Long> getLong = new ResultSetHandler<Long>() {
    public Long handle(ResultSet rs) throws SQLException {
      if (rs.next()) {
        return rs.getLong(1);
      } else {
        return null;
      }
    }
  };
  private final ResultSetHandler<List<SearchResult>> getResults =
      new ResultSetHandler<List<SearchResult>>() {
        public List<SearchResult> handle(ResultSet rs) throws SQLException {
          List<SearchResult> list = new ArrayList<>();
          while (rs.next()) {
            SearchResult result = new SearchResult();
            result.setGroupId(rs.getString(1));
            result.setArtifactId(rs.getString(2));
            result.setVersion(rs.getString(3));
            list.add(result);
          }
          return list;
        }
      };

  private final ResultSetHandler<SearchResult> getResult = new ResultSetHandler<SearchResult>() {
    public SearchResult handle(ResultSet rs) throws SQLException {
      SearchResult res = null;
      while (rs.next()) {
        if (res == null) {
          res = new SearchResult();
        }
        res.setGroupId(rs.getString(1));
        res.setArtifactId(rs.getString(2));
        res.setVersion(rs.getString(3));
        res.setRepoName(rs.getString(4));
        ResultType type = new ResultType();
        type.setName(rs.getString(5));
        type.setClassifier(rs.getString(6));
        res.getTypes().add(type);
      }
      return res;
    }
  };

  public SearchController(ConfigurationManager configManager) throws IOException {
    this.configManager = configManager;
    File plugins = new File(configManager.getRoot(), "plugins");
    System.setProperty("derby.system.home", plugins.getAbsolutePath());
    BasicDataSource s = new BasicDataSource();
    s.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
    s.setUrl("jdbc:derby:search;create=true");
    s.setUsername("su");
    s.setPassword("");
    s.setMaxTotal(10);
    s.setMinIdle(0);
    s.setDefaultAutoCommit(false);
    datasource = s;

    Set<String> allTables = new HashSet<>();
    Connection connection = null;

    try {
      try {
        connection = datasource.getConnection();
        DatabaseMetaData dbmeta = connection.getMetaData();
        try (ResultSet rs = dbmeta.getTables(null, null, null, new String[] {"TABLE"})) {
          while (rs.next()) {
            allTables.add(rs.getString("TABLE_NAME").toLowerCase());
          }
        }

        if (!allTables.contains("artifact")) {
          QueryRunner run = new QueryRunner();
          run.update(connection,
              "CREATE TABLE artifactindex(artifact bigint NOT NULL, version bigint NOT NULL)");
          run.update(connection, "INSERT INTO artifactindex (artifact,version) VALUES (?,?)", 1L,
              1L);
          run.update(connection,
              "CREATE TABLE artifact(id bigint NOT NULL,groupId character varying(120), artifactId character varying(120),CONSTRAINT artifact_pkey PRIMARY KEY (id))");
          run.update(connection,
              "CREATE TABLE artifactversion(artifact_id bigint NOT NULL,id bigint NOT NULL,"
                  + "version character varying(100)," + "reponame character varying(30),"
                  + "CONSTRAINT artifactversion_pkey PRIMARY KEY (id),"
                  + "CONSTRAINT fk_artifactversion_artifact_id FOREIGN KEY (artifact_id) REFERENCES artifact (id) )");
          run.update(connection,
              "CREATE TABLE artifacttype(version_id bigint NOT NULL,packaging character varying(20) NOT NULL,classifier character varying(30),"
                  + "CONSTRAINT artifacttype_pkey PRIMARY KEY (version_id,packaging,classifier),"
                  + "CONSTRAINT fk_artifacttype_version FOREIGN KEY (version_id) REFERENCES artifactversion (id))");
          run.update(connection, "CREATE INDEX artifactindex ON artifact(groupId,artifactId)");
          run.update(connection, "CREATE INDEX artifactgroupindex ON artifact(groupId)");
          run.update(connection, "CREATE INDEX artifactversionindex ON artifactversion(version)");
        }
        connection.commit();
      } catch (SQLException ex) {
        connection.rollback();
        throw ex;
      } finally {
        DbUtils.close(connection);
      }
    } catch (SQLException ex) {
      throw new IOException(ex);
    }
  }

  public void rebuild() throws IOException {
    final QueryRunner run = new QueryRunner();

    try {
      Connection connection = null;
      try {
        connection = datasource.getConnection();
        run.update(connection, "delete from artifacttype");
        run.update(connection, "delete from artifactversion");
        run.update(connection, "delete from artifact");
        run.update(connection, "update artifactindex set artifact=1,version=1");
        connection.commit();
      } catch (SQLException ex) {
        connection.rollback();
        throw ex;
      } finally {
        DbUtils.close(connection);
      }
    } catch (SQLException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new IOException(ex);
    }
    for (final Repository repository : configManager.getConfiguration().getRepositories()) {
      File repo = new File(configManager.getRoot(), repository.getName());
      if (repo.exists()) {
        Files.walkFileTree(repo.toPath(), new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            if (file.toString().endsWith(".pom")) {
              SearchResult result = new SearchResult();
              result.setRepoName(repository.getName());

              Model model;
              try (InputStream in = new FileInputStream(file.toFile())) {
                model = new MavenXpp3Reader().read(in);
              } catch (XmlPullParserException ex) {
                throw new IOException(ex);
              }
              
              if(model.getGroupId() == null && model.getParent() != null) {
                model.setGroupId(model.getParent().getGroupId());
              }
              
              if(model.getArtifactId() == null && model.getParent() != null) {
                model.setArtifactId(model.getParent().getArtifactId());
              }
              
              if(model.getVersion() == null && model.getParent() != null) {
                model.setVersion(model.getParent().getVersion());
              }

              result.setGroupId(model.getGroupId());
              result.setArtifactId(model.getArtifactId());
              result.setVersion(model.getVersion());
              ResultType pom = new ResultType();
              pom.setName("pom");
              // pom.setLink(buildLink(file.toFile()));
              result.getTypes().add(pom);
              File[] children = file.toFile().getParentFile().listFiles();
              if (children != null) {
                String base = result.getArtifactId() + "-" + result.getVersion();
                for (File child : children) {
                  String cname = child.getName();
                  if (!cname.endsWith(".md5") && !cname.endsWith(".sha1")
                      && !cname.endsWith(".notfound") && !cname.endsWith(".pom")
                      && cname.startsWith(base)) {
                    String end = cname.substring(base.length() + 1);
                    int split = end.indexOf('.');
                    ResultType type = new ResultType();
                    // type.setLink(buildLink(child));
                    if (split == -1) {
                      type.setName(end);
                    } else {
                      type.setName(end.substring(split + 1));
                      type.setClassifier(end.substring(0, split));
                    }
                  }
                }
              }
              System.out.println(result.getGroupId()+", "+result.getArtifactId()+", "+result.getVersion()+", "+result.getRepoName());
              
              try {
                Connection connection = null;
                try {
                  connection = datasource.getConnection();
                  Long artifactIdx = run.query(connection,
                      "select id from artifact where groupId=? and artifactId=?", getLong,
                      result.getGroupId(), result.getArtifactId());
                  if (artifactIdx == null) {
                    artifactIdx =
                        run.query(connection, "select artifact from artifactindex", getLong);
                    run.update(connection, "update artifactindex set artifact=?", artifactIdx + 1);
                    run.update(connection,
                        "insert into artifact (id,groupId,artifactId) VALUES (?,?,?)", artifactIdx,
                        result.getGroupId(), result.getArtifactId());
                  }
                  Long versionId =
                      run.query(connection, "select version from artifactindex", getLong);
                  run.update(connection, "update artifactindex set version=?", versionId + 1);
                  run.update(connection,
                      "insert into artifactversion(artifact_id,id,version,reponame) VALUES (?,?,?,?)",
                      artifactIdx, versionId, result.getVersion(), result.getRepoName());
                  for (ResultType res : result.getTypes()) {
                    run.update(connection,
                        "insert into artifacttype(version_id,packaging,classifier) VALUES (?,?,?)",
                        versionId, res.getName(), res.getClassifier());
                  }
                  connection.commit();
                } catch (SQLException ex) {
                  connection.rollback();
                  throw ex;
                } finally {
                  DbUtils.close(connection);
                }
              } catch (SQLException ex) {
                logger.log(Level.SEVERE, null, ex);
                throw new IOException(ex);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });
      }
    }
  }

  public SearchResult get(String groupId, String artifactId, String version) throws SQLException {
    return new QueryRunner(datasource).query(
        "select a.groupId,a.artifactId,v.version,v.reponame,t.packaging,t.classifier from artifact a "
            + "inner join artifactversion v on v.artifact_id = a.id "
            + "left join artifacttype t on t.version_id = v.id "
            + "where a.groupId=? and a.artifactId=? and v.version=?",
        getResult, groupId, artifactId, version);
  }

  public void set(SearchResult result) {

  }

  public List<SearchResult> searchByGroupId(String groupId, int first) throws SQLException {
    return new QueryRunner(datasource)
        .query("select a.groupId,a.artifactId,max(v.version) from artifact a "
            + "inner join artifactversion v on v.artifact_id = a.id where a.groupId=? group by a.groupId,a.artifactId order by a.groupId,a.artifactId OFFSET "
            + first + " ROWS FETCH NEXT 20 ROWS ONLY", getResults, groupId);
  }

  public long countByGroupId(String groupId) throws SQLException {
    return new QueryRunner(datasource).query("select count(a.id) from artifact a where a.groupId=?",
        getLong, groupId);
  }

  public List<SearchResult> searchByArtifactId(String groupId, String artifactId, int first)
      throws SQLException {
    return new QueryRunner(datasource)
        .query("select a.groupId,a.artifactId,v.version from artifact a "
            + "inner join artifactversion v on v.artifact_id = a.id where a.groupId=? and a.artifactId=? order by a.groupId,a.artifactId,v.version desc OFFSET "
            + first + " ROWS FETCH NEXT 20 ROWS ONLY", getResults, groupId, artifactId);
  }

  public long countByArtifactId(String groupId, String artifactId) throws SQLException {
    return new QueryRunner(datasource).query(
        "select count(v.id) from artifact a inner join artifactversion v on v.artifact_id = a.id where a.groupId=? and a.artifactId=?",
        getLong, groupId, artifactId);
  }

  public List<SearchResult> search(String text, int first) throws SQLException {
    String textTrimed = text.trim();
    if (textTrimed.isEmpty()) {
      return new QueryRunner(datasource)
          .query("select a.groupId,a.artifactId,max(v.version) from artifact a "
              + "inner join artifactversion v on v.artifact_id = a.id group by a.groupId,a.artifactId order by a.groupId,a.artifactId OFFSET "
              + first + " ROWS FETCH NEXT 20 ROWS ONLY", getResults);
    }
    return searchQuery("select a.groupId,a.artifactId,max(v.version) from artifact a "
        + "inner join artifactversion v on v.artifact_id = a.id where %s group by a.groupId,a.artifactId OFFSET "
        + first + " ROWS FETCH NEXT 20 ROWS ONLY", textTrimed, getResults);
  }

  public long count(String text) throws SQLException {
    String textTrimed = text.trim();
    if (textTrimed.isEmpty()) {
      return new QueryRunner(datasource).query("select count(a.id) from artifact a", getLong);
    }
    return searchQuery("select count(a.id) from artifact a where %s", textTrimed, getLong);
  }

  private <T> T searchQuery(String sqlPattern, String text, ResultSetHandler<T> handler)
      throws SQLException {
    String[] parts = text.split("\\s");
    StringBuilder cond = new StringBuilder();
    List<String> arguments = new ArrayList<>();
    for (String part : parts) {
      String trimed = part.trim();
      if (trimed.isEmpty()) {
        continue;
      }
      if (cond.length() > 0) {
        cond.append(" and ");
      }
      cond.append("(a.groupId like ? or a.artifactId like ?)");
      trimed = "%" + trimed + "%";
      arguments.add(trimed);
      arguments.add(trimed);
    }
    return new QueryRunner(datasource).query(String.format(sqlPattern, cond.toString()), handler,
        arguments.toArray());
  }

}
