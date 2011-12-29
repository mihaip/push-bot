package info.persistent.pushbot.data;

import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.xmpp.JID;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(
    identityType = IdentityType.APPLICATION, detachable = "true")
public class Subscription {
  private static final int MAX_SEEN_ENTRY_SIZE = 1000;
  
  private static final Logger logger =
      Logger.getLogger(Subscription.class.getName());

  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Long id;
  
  @Persistent
  private String user;
  
  @Persistent(defaultFetchGroup = "true")
  private Link feedUrl;
  
  @Persistent(defaultFetchGroup = "true")
  private Link hubUrl;
  
  @Persistent
  private String title;
  
  @Persistent(defaultFetchGroup = "true")
  private Set<String> seenEntryIds;
  
  public Subscription(JID user, URL feedUrl, URL hubUrl, String title) {
    this.user = Xmpp.toShortJid(user).getId();
    this.feedUrl = new Link(feedUrl.toString());
    this.hubUrl = new Link (hubUrl.toString());
    this.title = title;
    this.seenEntryIds = Sets.newHashSet();
  }
  
  public Long getId() {
    return id;
  }
  
  public JID getUser() {
    return new JID(user);
  }
  
  public URL getFeedUrl() {
    try {
      return new URL(feedUrl.getValue());
    } catch (MalformedURLException err) {
      // All stored URLs should be valid, since we're creating them from URL
      // instances
      throw new RuntimeException(err);
    }
  }
  
  public URL getHubUrl() {
    try {
      return new URL(hubUrl.getValue());
    } catch (MalformedURLException err) {
      // Ditto
      throw new RuntimeException(err);
    }
  }
  
  public String getTitle() {
    return title;
  }
  
  public ImmutableSet<String> getSeenEntryIds() {
    return seenEntryIds != null
        ? ImmutableSet.<String>copyOf(seenEntryIds)
        : ImmutableSet.<String>of();
  }
  
  public void addSeenEntryId(String entryId) {
    if (seenEntryIds == null) {
      seenEntryIds = Sets.newHashSet();
    }
    if (seenEntryIds.size() >= MAX_SEEN_ENTRY_SIZE - 1) {
      logger.warning("Subscription " + feedUrl.getValue() + " for " + user + 
          " had " + seenEntryIds.size() + " entries, dropping some");
      // Ideally we'd drop the oldest entries, but we don't store timestamps...
      seenEntryIds = Sets.newHashSet(
          Lists.newArrayList(seenEntryIds).subList(0, MAX_SEEN_ENTRY_SIZE - 1));
    }
    seenEntryIds.add(entryId);
  }
  
  public static List<Subscription> getSubscriptionsForUser(JID user) {
    final String queryUserId = Xmpp.toShortJid(user).getId();
    final List<Subscription> result = Lists.newArrayList();
    Persistence.withManager(new Persistence.Closure() {
      @SuppressWarnings("unchecked")
      @Override public void run(PersistenceManager manager) {
        Query query = manager.newQuery(Subscription.class);
        query.setFilter("user == userParam");
        query.declareParameters("String userParam");
        result.addAll((List<Subscription>) query.execute(queryUserId));
        query.closeAll();
      }
    });
    
    return result;
  }
  
  public static List<Subscription> getSubscriptionsForUserAndFeedUrl(
      JID user, URL feedUrl) {
    final String queryUserId = Xmpp.toShortJid(user).getId();
    final Link queryFeedUrl = new Link(feedUrl.toString());
    final List<Subscription> result = Lists.newArrayList();
    Persistence.withManager(new Persistence.Closure() {
      @SuppressWarnings("unchecked")
      @Override public void run(PersistenceManager manager) {
        Query query = manager.newQuery(Subscription.class);
        query.setFilter("user == userParam && feedUrl == feedUrlParam");
        query.declareParameters("String userParam, " +
            "com.google.appengine.api.datastore.Link feedUrlParam");
        result.addAll(
            (List<Subscription>) query.execute(queryUserId, queryFeedUrl));
        query.closeAll();
      }
    });
    
    return result;
  }  
}
