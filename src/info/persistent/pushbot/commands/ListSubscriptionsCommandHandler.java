package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.repackaged.com.google.common.base.StringUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Xmpp;

import java.util.List;

public class ListSubscriptionsCommandHandler implements CommandHandler {

  @Override
  public void handle(JID user, String... args) {
    List<Subscription> subscriptions =
        Subscription.getSubscriptionsForUser(user);
    if (subscriptions.isEmpty()) {
      Xmpp.sendMessage(user, "You have no subscriptions.");
      return;
    }
    
    Multimap<String, Subscription> subscriptionsByHub = ArrayListMultimap.create();
    for (Subscription subscription : subscriptions) {
      subscriptionsByHub.put(subscription.getHubUrl().toString(), subscription);
    }
    
    StringBuilder message = new StringBuilder("Subscriptions:");
    
    for (String hubUrl : subscriptionsByHub.keySet()) {
      message.append("\n  at hub ").append(hubUrl).append(":");
      for (Subscription subscription : subscriptionsByHub.get(hubUrl)) {
        System.err.println("Seen entry IDs: " + subscription.getSeenEntryIds());
        message.append("\n    ").append(subscription.getFeedUrl());
        String title = subscription.getTitle();
        if (title != null && !title.isEmpty()) {
          message.append(" (")
              .append(StringUtil.unescapeHTML(title)).append(")");
        }
      }
    }
    
    Xmpp.sendMessage(user, message.toString());
  }

}
