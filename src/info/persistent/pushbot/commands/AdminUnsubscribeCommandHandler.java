// Copyright 2011 Google Inc. All Rights Reserved.

package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Hubs;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

/**
 * Remove another user's subscription.
 */
public class AdminUnsubscribeCommandHandler extends AdminCommandHandler {
  private static final Logger logger =
      Logger.getLogger(AdminUnsubscribeCommandHandler.class.getName());

  @Override
  public void handle(JID adminUser, JID targetUser, String... args) {
    if (args.length != 1) {
      Xmpp.sendMessage(adminUser, "No feed URL not specified");
      return;
    }
    
    URL feedUrl;
    try {
      feedUrl = new URL(args[0]);
    } catch (MalformedURLException err) {
      logger.log(Level.INFO, "URL parse exception", err);
      Xmpp.sendMessage(adminUser, "Feed URL is malformed");
      return;
    }

    final List<Subscription> subscriptions =
        Subscription.getSubscriptionsForUserAndFeedUrl(targetUser, feedUrl);
      
    if (subscriptions.isEmpty()) {
      Xmpp.sendMessage(adminUser, "No subscriptions match.");
    }
    
    for (Subscription subscription : subscriptions) {
      Hubs.sendRequestToHub(targetUser, subscription.getHubUrl(), feedUrl, false);
    }
    
    Persistence.withManager(new Persistence.Closure() {
      @Override public void run(PersistenceManager manager) {
        manager.deletePersistentAll(subscriptions);
      }
    });
    
    Xmpp.sendMessage(adminUser, "Removed " + subscriptions.size() + " subscriptions.");
  }

}
