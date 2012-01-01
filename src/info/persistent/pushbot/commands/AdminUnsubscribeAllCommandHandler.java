// Copyright 2011 Google Inc. All Rights Reserved.

package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Hubs;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.util.List;

import javax.jdo.PersistenceManager;

/**
 * Remove another user's subscriptions.
 */
public class AdminUnsubscribeAllCommandHandler extends AdminCommandHandler {

  @Override protected void handle(JID adminUser, JID targetUser, String... args) {
    final List<Subscription> subscriptions =
        Subscription.getSubscriptionsForUser(targetUser);
      
    if (subscriptions.isEmpty()) {
      Xmpp.sendMessage(adminUser, "No subscriptions match.");
    }
    
    for (Subscription subscription : subscriptions) {
      Hubs.sendRequestToHub(
        targetUser, subscription.getHubUrl(), subscription.getFeedUrl(), false);
    }
    
    Persistence.withManager(new Persistence.Closure() {
      @Override public void run(PersistenceManager manager) {
        manager.deletePersistentAll(subscriptions);
      }
    });    

    Xmpp.sendMessage(adminUser, "Removed " + subscriptions.size() + " subscriptions.");
  }
}
