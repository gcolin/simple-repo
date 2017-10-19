package net.gcolin.simplerepo.business;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.gcolin.simplerepo.model.SearchResult;

public class UpdateIndexManager {

  private ServletContext context;

  public UpdateIndexManager(ServletContext context) {
    this.context = context;
  }
  
  public void publish(SearchResult result) {
    HttpURLConnection conn = null;
    try {
      Configuration config = (Configuration) context.getAttribute("freemarkerConfiguration");
      Template template = config.getTemplate("/WEB-INF/search/put.ftl");
      StringWriter sw = new StringWriter();
      template.process(result, sw);

      ConfigurationManager configManager =
          (ConfigurationManager) context.getAttribute("configManager");
      
      URL url = new URL(configManager.getElasticsearch() + "/maven/" + result.getRepoName() + "/" + result.toString());
      conn = (HttpURLConnection) url.openConnection();
      conn.setDoInput(true);
      conn.setRequestMethod("PUT");
      conn.getOutputStream().write(sw.toString().getBytes(StandardCharsets.UTF_8));
      conn.getOutputStream().flush();
      IOUtils.toByteArray(conn.getInputStream());
    } catch (TemplateException|IOException ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.close(conn);
    }
    
  }
  
}
