package info.persistent.pushbot;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.repackaged.com.google.common.base.StringUtil;
import com.google.common.collect.Lists;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import info.persistent.pushbot.data.Subscription;
import info.persistent.pushbot.util.Feeds;
import info.persistent.pushbot.util.Persistence;
import info.persistent.pushbot.util.Xmpp;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class PushSubscriberServlet extends HttpServlet {
  private static final Logger logger =
      Logger.getLogger(PushSubscriberServlet.class.getName());
  
  private static final int MAX_ENTRIES_TO_DISPLAY = 3;

  /** Subscription verifications arrive via GETs */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setStatus(200);
    resp.setContentType("text/plain");
    resp.getOutputStream().print(req.getParameter("hub.challenge"));
    resp.getOutputStream().flush();

    JID user = new JID(req.getPathInfo().substring(1));

    if (req.getParameter("hub.mode").equals("subscribe")) {
      Xmpp.sendMessage(user, "Subscribed to " + req.getParameter("hub.topic"));
    } else if (req.getParameter("hub.mode").equals("unsubscribe")) {
      Xmpp.sendMessage(
          user, "Unsubscribed from " + req.getParameter("hub.topic"));      
    }
  }

  /** Actual notifications arrive via POSTs */
  @SuppressWarnings("unchecked")
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    resp.setStatus(204);

    SyndFeed feed = Feeds.parseFeed(req.getInputStream());
    if (feed == null) {
      return;
    }
    
    List<SyndEntry> entries = feed.getEntries();
    
    if (entries.isEmpty()) {
      return;
    }    
    
    JID user = new JID(req.getPathInfo().substring(1));
    // TODO(mihaip): this is potentially incorrect if a feed gets redirected and
    // its self URL changes since the time the subscription was created
    List<URL> feedUrls = Feeds.getLinkUrl(feed, Feeds.SELF_RELATION); 
    if (!feedUrls.isEmpty()) {
      URL feedUrl = feedUrls.get(0);
      List<Subscription> subscriptions =
        Subscription.getSubscriptionsForUserAndFeedUrl(user, feedUrl);
      
      if (!subscriptions.isEmpty()) {
        final Subscription subscription = subscriptions.get(0);
        Set<String> seenEntryIds = subscription.getSeenEntryIds();
        List<SyndEntry> filteredEntries = Lists.newArrayList();
        for (SyndEntry entry : entries) {
          String entryId = Feeds.getEntryId(entry);
          if (seenEntryIds.contains(entryId)) {
            logger.info("Filtering out already seen entry from " + feedUrl);
            continue;
          }
          filteredEntries.add(entry);
          subscription.addSeenEntryId(entryId);
        }
        
        if (!filteredEntries.isEmpty()) {
          Persistence.withManager(new Persistence.Closure() {
            @Override public void run(PersistenceManager manager) {
              manager.makePersistent(subscription);
            }
          });
        } else {
          return;
        }
        
        entries = filteredEntries;
      }
    }
    
 
    // If subscribing to a previously unseen URL, the hub might report a bunch
    // of entries as new, so we sort them by published date and only show the
    // first few
    Collections.sort(entries, new Comparator<SyndEntry>() {
      @Override public int compare(SyndEntry o1, SyndEntry o2) {
        if (o1.getPublishedDate() == null) {
          return 1;
        }
        if (o2.getPublishedDate() == null) {
          return -1;
        }
        return o2.getPublishedDate().compareTo(o1.getPublishedDate());
      }
    });
    
    List<SyndEntry> displayEntries;
    if (entries.size() > MAX_ENTRIES_TO_DISPLAY) {
      displayEntries = entries.subList(0, MAX_ENTRIES_TO_DISPLAY);
    } else {
      displayEntries = entries;
    }
    
    StringBuilder message = new StringBuilder("Update from ")
        .append(StringUtil.unescapeHTML(feed.getTitle())).append(":");
    for (SyndEntry displayEntry : displayEntries) {
      String title = displayEntry.getTitle();
      if (StringUtil.isEmptyOrWhitespace(title)) {
        title = "(title unknown)";
      } else {
        title = StringUtil.unescapeHTML(title);
      }
      String link = displayEntry.getLink();
      if (StringUtil.isEmptyOrWhitespace(link)) {
        link = "<no link>";
      }
      
      message.append("\n  ").append(title).append(": ").append(link);
    }
    
    if (displayEntries.size() != entries.size()) {
      message.append("\n  (and ")
        .append(entries.size() - displayEntries.size()).append(" more)");
    }
    
    Xmpp.sendMessage(user, message.toString());
  }
}
