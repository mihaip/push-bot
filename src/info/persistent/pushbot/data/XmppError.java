// Copyright 2011 Google Inc. All Rights Reserved.

package info.persistent.pushbot.data;

import com.google.appengine.api.xmpp.JID;
import com.google.common.collect.Lists;

import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.util.List;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Keeps track of XMPP errors seen for users.
 */
@PersistenceCapable(
  identityType = IdentityType.APPLICATION, detachable = "true")
public class XmppError {
  @PrimaryKey
  @Persistent
  private String user;
  
  @Persistent
  @Extension(vendorName = "datanucleus", key = "gae.unindexed", value="true")
  private int errorCount;

  public XmppError(JID user) {
    this.user = Xmpp.toShortJid(user).getId();
    this.errorCount = 0;
  }
  
  public static XmppError getOrCreateForUser(JID user) {
    final String queryUserId = Xmpp.toShortJid(user).getId();
    final List<XmppError> result = Lists.newArrayList();
    Persistence.withManager(new Persistence.Closure() {
      @Override public void run(PersistenceManager manager) {
        try {
          XmppError error = manager.getObjectById(XmppError.class, queryUserId);
          if (error != null) {
            result.add(error);
          }
        } catch (JDOObjectNotFoundException err) {
          // Ignore, we will construct a new XmppError in the response
        }
      }
    });
    
    if (!result.isEmpty()) {
      return result.get(0);
    }
    
    return new XmppError(user);
  }
  
  public JID getUser() {
    return new JID(user);
  }
  
  public int getErrorCount() {
    return errorCount;
  }
  
  public void incrementErrorCount() {
    errorCount++;
  }
}
