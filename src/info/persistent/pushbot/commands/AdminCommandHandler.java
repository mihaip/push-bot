// Copyright 2011 Google Inc. All Rights Reserved.

package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.util.Xmpp;

import java.util.Arrays;

/**
 * Base class for commands that require administrator access.
 */
public abstract class AdminCommandHandler implements CommandHandler {
  
  public static boolean isAdmin(JID user) {
    return Xmpp.toShortJid(user).getId().equals("mihai.parparita@gmail.com");
  }

  @Override
  public void handle(JID adminUser, String... args) {
    if (!isAdmin(adminUser)) {
      Xmpp.sendMessage(adminUser, "You're not an administrator");
      return;
    }
    
    if (args.length == 0) {
      Xmpp.sendMessage(adminUser, "Need arguments");
      return;
    }    
    
    JID targetUser = new JID(args[0]);
    
    handle(adminUser, targetUser, Arrays.copyOfRange(args, 1, args.length));
  }
  
  protected abstract void handle(JID adminUser, JID targetUser, String... args);
}
