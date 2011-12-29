package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Hubs;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.net.URL;
import java.util.List;

import javax.jdo.PersistenceManager;

public class UnsubscribeCommandHandler extends FeedCommandHandler {

  @Override
  protected void handle(JID user, URL feedUrl) {
    final List<Subscription> subscriptions =
      Subscription.getSubscriptionsForUserAndFeedUrl(user, feedUrl);
    
    if (subscriptions.isEmpty()) {
      Xmpp.sendMessage(user, "No subscriptions match.");
    }
    
    for (Subscription subscription : subscriptions) {
      Hubs.sendRequestToHub(user, subscription.getHubUrl(), feedUrl, false);
    }
    
    Persistence.withManager(new Persistence.Closure() {
      @Override public void run(PersistenceManager manager) {
        manager.deletePersistentAll(subscriptions);
      }
    });
  }

}
