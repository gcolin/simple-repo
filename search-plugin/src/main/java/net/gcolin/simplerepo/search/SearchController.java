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
package net.gcolin.simplerepo.search;

import net.gcolin.simplerepo.maven.MavenUtil;
import net.gcolin.simplerepo.model.Repository;
import net.gcolin.simplerepo.model.ResultType;
import net.gcolin.simplerepo.model.SearchResult;
import net.gcolin.simplerepo.util.ConfigurationManager;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO for search.
 * 
 * @author Gaël COLIN
 * @since 1.0
 */
public class SearchController extends MavenUtil {

  private final static String REBUILD = "Building index: ";
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private BasicDataSource datasource;
  private ConfigurationManager configManager;
  private volatile boolean running = false;
  private int nb = 0;
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
    plugins.mkdirs();
    System.setProperty("derby.system.home", plugins.getAbsolutePath());
    BasicDataSource s = new BasicDataSource();
    s.setDriverClassName("org.apache.derby.jdbc.EmbeddedDriver");
    s.setUrl("jdbc:derby:search" + (new File(plugins, "search").exists() ? "" : ";create=true"));
    s.setUsername("su");
    s.setPassword("");
    s.setMaxTotal(10);
    s.setMinIdle(0);
    s.setDefaultAutoCommit(true);
    datasource = s;

    Set<String> allTables = new HashSet<>();
    Connection connection = null;

    try {
      try {
        connection = datasource.getConnection();
        connection.setAutoCommit(false);
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
    if (running) {
      return;
    }
    running = true;
    configManager.setCurrentAction(REBUILD + "initialize");
    nb = 0;
    try {
      final QueryRunner run = new QueryRunner();

      try {
        Connection connection = null;
        try {
          connection = datasource.getConnection();
          connection.setAutoCommit(false);
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
              nb++;
              if (nb % 20 == 0) {
                configManager.setCurrentAction(REBUILD + " " + nb + " files");
              }
              if (file.toString().endsWith(".pom")) {
                Model model = readPom(file.toFile());
                add(repository, file.toFile(), model);
              }
              return FileVisitResult.CONTINUE;
            }

          });
        }
      }
    } finally {
      running = false;
      configManager.setCurrentAction(null);
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

  public void close() {
    try {
      datasource.close();
      datasource = null;
      DriverManager.getConnection("jdbc:derby:search;shutdown=true");
    } catch (SQLException ex) {
      logger.log(Level.FINE, null, ex);
    }
  }

  public void remove(Model model) throws SQLException {
    new QueryRunner(datasource).update(
        "delete t from artifacttype t inner join artifactversion v on v.id = t.version_id "
            + "inner join artifact a on a.id = v.artifact_id where a.groupId = ? and a.artifactId = ? and v.version = ?",
        model.getGroupId(), model.getArtifactId(), model.getVersion());
    new QueryRunner(datasource).update(
        "delete v from artifactversion v on v.id = t.version_id "
            + "inner join artifact a on a.id = v.artifact_id where a.groupId = ? and a.artifactId = ? and v.version = ?",
        model.getGroupId(), model.getArtifactId(), model.getVersion());
  }

  public void remove(Model model, ResultType type) throws SQLException {
    new QueryRunner(datasource).update(
        "delete t from artifacttype t inner join artifactversion v on v.id = t.version_id "
            + "inner join artifact a on a.id = v.artifact_id where a.groupId = ? and a.artifactId = ? and v.version = ? and t.packaging = ? and t.classifier = ?",
        model.getGroupId(), model.getArtifactId(), model.getVersion(), type.getLink(),
        type.getClassifier());
  }

  public void add(Model model, ResultType type) throws SQLException {
    Connection connection = null;
    try {
      connection = datasource.getConnection();
      connection.setAutoCommit(false);
      Long versionId = new QueryRunner().query(connection,
          "select v.id from artifactversion v inner join artifact a on a.id = v.artifact_id "
              + "where a.groupId = ? and a.artifactId = ? and v.version = ?",
          getLong, model.getGroupId(), model.getArtifactId(), model.getVersion());
      new QueryRunner().update(connection,
          "insert into artifacttype(version_id,packaging,classifier) VALUES (?,?,?)", versionId,
          type.getName(), type.getClassifier());
      connection.commit();
    } catch (SQLException ex) {
      connection.rollback();
      throw ex;
    } finally {
      DbUtils.close(connection);
    }
  }

  public void add(final Repository repository, File pomFile, Model model) throws IOException {
    SearchResult result = buildResult(repository.getName(), pomFile, model);
    try {
      Connection connection = null;
      try {
        connection = datasource.getConnection();
        connection.setAutoCommit(false);
        QueryRunner run = new QueryRunner();
        Long artifactIdx =
            run.query(connection, "select id from artifact where groupId=? and artifactId=?",
                getLong, result.getGroupId(), result.getArtifactId());
        if (artifactIdx == null) {
          artifactIdx = run.query(connection, "select artifact from artifactindex", getLong);
          run.update(connection, "update artifactindex set artifact=?", artifactIdx + 1);
          run.update(connection, "insert into artifact (id,groupId,artifactId) VALUES (?,?,?)",
              artifactIdx, result.getGroupId(), result.getArtifactId());
        }
        Long versionId = run.query(connection, "select version from artifactindex", getLong);
        run.update(connection, "update artifactindex set version=?", versionId + 1);
        run.update(connection,
            "insert into artifactversion(artifact_id,id,version,reponame) VALUES (?,?,?,?)",
            artifactIdx, versionId, result.getVersion(), result.getRepoName());
        for (ResultType res : result.getTypes()) {
          run.update(connection,
              "insert into artifacttype(version_id,packaging,classifier) VALUES (?,?,?)", versionId,
              res.getName(), res.getClassifier());
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
}
