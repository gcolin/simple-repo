package net.gcolin.server.maven;

import java.io.File;


public interface PluginContainer {

  void add(PluginListener pluginListener);

  void installPlugin(File file);

  void removePlugin(File file);

}
