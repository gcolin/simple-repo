package net.gcolin.simplerepo.maven;

public interface DisplayLink {

  boolean isAvailable(String group, String artifact, String version);
  
  String getExactLink(String group, String artifact, String version);
  
}
