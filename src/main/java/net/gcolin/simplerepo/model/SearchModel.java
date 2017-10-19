package net.gcolin.simplerepo.model;

public class SearchModel {

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String query;
  private final int offset;
  private final int max;
  
  public SearchModel(String groupId, String artifactId, String version, String query, int offset,
      int max) {
    super();
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.query = query;
    this.offset = offset;
    this.max = max;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getQuery() {
    return query;
  }
  
  public int getOffset() {
    return offset;
  }

  public int getMax() {
    return max;
  }

  public String getMust() {
    StringBuilder str = new StringBuilder();
    if (groupId != null) {
      str.append("{\"term\":{\"groupId\":\"" + groupId + "\"},");
    }
    if (artifactId != null) {
      str.append("{\"term\":{\"artifactId\":\"" + artifactId + "\"},");
    }
    if (version != null) {
      str.append("{\"term\":{\"version\":\"" + version + "\"},");
    }
    if (query != null) {
      str.append("{\"query_string\":{\"query\":\"" + query + "\"},");
    }
    str.delete(str.length() - 1, str.length());
    return str.toString();
  }

}
