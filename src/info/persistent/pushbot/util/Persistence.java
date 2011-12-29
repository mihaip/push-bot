package info.persistent.pushbot.util;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public class Persistence {
  private static final PersistenceManagerFactory managerFactory =
    JDOHelper.getPersistenceManagerFactory("transactions-optional");

  private Persistence() {
    // Not meant to be instantiable
  }
  
  public static void withManager(Closure closure) {
    PersistenceManager manager = managerFactory.getPersistenceManager();
    try {
      closure.run(manager);
    } finally {
      manager.close();
    }
  }
  
  public static interface Closure {
    void run(PersistenceManager manager);
  }
}
