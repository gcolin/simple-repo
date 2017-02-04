package net.gcolin.server.maven.plugin.search.test;

import net.gcolin.server.maven.plugin.search.Resolver;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class ResolverTest {

  @Test
  public void testProps() {
    Properties properties = new Properties();
    properties.put("jetty.version", "hello");
    Assert.assertEquals("hello", Resolver.resolve("${jetty.version}", properties, new Model()));
    Assert.assertEquals("1hello", Resolver.resolve("1${jetty.version}", properties, new Model()));
    Assert.assertEquals("1hello2", Resolver.resolve("1${jetty.version}2", properties, new Model()));
    Assert.assertEquals("hello2", Resolver.resolve("${jetty.version}2", properties, new Model()));
  }
  
  @Test
  public void testModel() {
    Properties properties = new Properties();
    Model model = new Model();
    model.setArtifactId("artifactName");
    Parent parent = new Parent();
    parent.setVersion("2.1");
    model.setParent(parent);
    Assert.assertEquals("artifactName", Resolver.resolve("${project.artifactId}", properties, model));
    Assert.assertEquals("2.1", Resolver.resolve("${project.parent.version}", properties, model));
  }
  
}
