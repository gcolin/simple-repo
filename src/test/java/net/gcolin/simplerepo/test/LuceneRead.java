package net.gcolin.simplerepo.test;

import net.gcolin.simplerepo.business.ConfigurationManager;

public class LuceneRead {

  public static void main(String[] args) throws Exception {
    ConfigurationManager configManager = new ConfigurationManager(null);
    //SearchManager manager = new SearchManager(configManager);
    /*for(int i=102;i<110;i++) {
      System.out.println(i);
    Document d = manager.getCentralContext().acquireIndexSearcher().doc(i);
    for(Fieldable f:  d.getFields()) {
      System.out.println(f.name() + " : " + d.get(f.name()));
      
    }
    }*/
//    Result r = manager.search(null, null, "common", 0);
//    System.out.println(r.getList().size());
//    manager.close();
  }

}
