package net.gcolin.simplerepo.business;

import java.io.IOException;
import java.net.URL;

public class NexusIndexReader {
  
  
  
  public void updateIndex(String repoName, int port, String contextPath) {
    try {
      URL url = new URL("http://localhost:" + port + (contextPath.length() > 1 ? contextPath : "") + "/");
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
