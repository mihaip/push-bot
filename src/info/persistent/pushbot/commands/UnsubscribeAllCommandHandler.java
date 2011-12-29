package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Hubs;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.util.List;

import javax.jdo.PersistenceManager;

public class UnsubscribeAllCommandHandler implements CommandHandler {

  @Override
  public void handle(JID user, String... args) {
    final List<Subscription> subscriptions =
      Subscription.getSubscriptionsForUser(user);
    
    if (subscriptions.isEmpty()) {
      Xmpp.sendMessage(user, "No subscriptions match.");
    }
    
    for (Subscription subscription : subscriptions) {
      Hubs.sendRequestToHub(
          user, subscription.getHubUrl(), subscription.getFeedUrl(), false);
    }
    
    Persistence.withManager(new Persistence.Closure() {
      @Override public void run(PersistenceManager manager) {
        manager.deletePersistentAll(subscriptions);
      }
    });
  }

}
