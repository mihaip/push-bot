package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;
import com.google.common.collect.ImmutableSet;

import com.sun.syndication.feed.synd.SyndFeed;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Feeds;
import info.persistent.pushbot.util.Hubs;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;

public class SubscribeCommandHandler extends FeedCommandHandler {
  public static final Logger logger =
      Logger.getLogger(SubscribeCommandHandler.class.getName());
  
  private static final Set<String> FEEDBURNER_HOSTS = ImmutableSet.of(
      "feeds.feedburner.com",
      "feeds2.feedburner.com",
      "feedproxy.google.com");
  private static final String FEEDBURNER_HUB_HOST = "pubsubhubbub.appspot.com";
  
  @Override
  protected void handle(JID user, URL feedUrl) {
    subscribeToFeed(user, feedUrl);
  }
  
  static void subscribeToFeed(JID user, URL feedUrl) {
    SyndFeed feed = fetchAndParseFeed(user, feedUrl);
    if (feed == null) {
      return;
    }
    
    // If possible, subscribe to the self URL, since presumably that's the one
    // that the hub knows about.
    List<URL> selfUrls = Feeds.getLinkUrl(feed, Feeds.SELF_RELATION);
    if (!selfUrls.isEmpty()) {
      feedUrl = selfUrls.get(0);
    }

    List<URL> hubUrls = Feeds.getLinkUrl(feed, Feeds.HUB_RELATION);
    URL hubUrl = null;
    
    for (URL candidateHubUrl : hubUrls) {
      // Require absolute URLs
      if (candidateHubUrl.getHost() == null ||
          candidateHubUrl.getHost().isEmpty()) {
        continue;
      }
      
      // If it's a burned feed, then we want to use the FeedBurner hub, not
      // another one (which doesn't know about the burned feed URL). This can
      // happen with TypePad.
      if (FEEDBURNER_HOSTS.contains(feedUrl.getHost()) &&
          !candidateHubUrl.getHost().equals(FEEDBURNER_HUB_HOST)) {
        continue;
      }
      
      hubUrl = candidateHubUrl;
      continue;
    }

    if (hubUrl == null) {
      Xmpp.sendMessage(
          user, "The feed " + feedUrl + " is not associated with a hub");
      return;      
    }

    saveSubscription(user, feedUrl, hubUrl, feed.getTitle());

    Hubs.sendRequestToHub(user, hubUrl, feedUrl, true);
  }

  private static SyndFeed fetchAndParseFeed(JID user, URL feedUrl) {
    SyndFeed feed;
    try {
      feed = Feeds.parseFeed(feedUrl.openStream());
    } catch (IOException err) {
      logger.log(Level.INFO, "Could not fetch feed " + feedUrl, err);
      Xmpp.sendMessage(user, "Could not fetch feed " + feedUrl);
      return null;
    }
    if (feed == null) {
      Xmpp.sendMessage(user, "Could not parse feed " + feedUrl);      
    }
    return feed;
  }

  private static void saveSubscription(
      JID user, URL feedUrl, URL hubUrl, String title) {
    List<Subscription> existingSubscriptions =
      Subscription.getSubscriptionsForUserAndFeedUrl(user, feedUrl);
    if (!existingSubscriptions.isEmpty()) {
      Xmpp.sendMessage(user, "You're already subscribed to " + feedUrl +
          " (sending request to hub anyway, in case it's out of sync)");
      return;
    }
    
    final Subscription subscription =
      new Subscription(user, feedUrl, hubUrl, title);
    Persistence.withManager(new Persistence.Closure() {
      @Override
      public void run(PersistenceManager manager) {
        manager.makePersistent(subscription);
      }
    });
  }

}
