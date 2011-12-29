package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;
import com.google.common.collect.Lists;

import com.sun.syndication.feed.opml.Opml;
import com.sun.syndication.feed.opml.Outline;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import com.sun.syndication.io.XmlReader;

import info.persistent.pushbot.util.Xmpp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpmlImportCommandHandler implements CommandHandler {
  private static final Logger logger =
      Logger.getLogger(OpmlImportCommandHandler.class.getName());

  @SuppressWarnings("unchecked")
  @Override
  public void handle(JID user, String... args) {
    if (args.length != 1) {
      Xmpp.sendMessage(user, "OPML URL not specified");
      return;
    }

    URL opmlUrl;
    try {
      opmlUrl = new URL(args[0]);
    } catch (MalformedURLException err) {
      logger.log(Level.INFO, "URL parse exception", err);
      Xmpp.sendMessage(user, "OPML URL is malformed");
      return;
    }
    
    Opml opml = fetchAndParseOpml(user, opmlUrl);
    if (opml == null) {
      return;
    }
    
    List<Outline> outlines = opml.getOutlines();
    List<URL> feedUrls = Lists.newArrayList();
    
    extractFeedUrls(outlines, feedUrls);
    
    if (feedUrls.isEmpty()) {
      Xmpp.sendMessage(user, "OPML file did not contain any feeds");
      return;
    }
    
    Xmpp.sendMessage(user, "Found " + feedUrls.size() +
        " feeds in OPML file, attempting to subscribe to them.");
    
    for (URL feedUrl : feedUrls) {
      SubscribeCommandHandler.subscribeToFeed(user, feedUrl);
    }
  }
  
  @SuppressWarnings("unchecked")
  private static void extractFeedUrls(
      List<Outline> outlines, List<URL> feedUrls) {
    for (Outline outline : outlines) {
      if (outline.getXmlUrl() != null) {
        try {
          feedUrls.add(new URL(outline.getXmlUrl()));
        } catch (MalformedURLException e) {
          logger.log(Level.INFO, "Malformed OPML URL", e);
          // TODO Auto-generated catch block
          continue;
        }
      }
      
      if (outline.getChildren() != null) {
        extractFeedUrls(outline.getChildren(), feedUrls);
      }
    }
  }
  
  private static Opml fetchAndParseOpml(JID user, URL opmlUrl) {
    WireFeedInput input = new WireFeedInput();
    try {
      return (Opml) input.build(new XmlReader(opmlUrl));
    } catch (IllegalArgumentException e) {
      handleOpmlParseException(user, e);
      return null;
    } catch (FeedException e) {
      handleOpmlParseException(user, e);
      return null;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      handleOpmlParseException(user, e);
      return null;
    }
  }
  
  private static void handleOpmlParseException(JID user, Throwable t) {
    logger.log(Level.INFO, "OPML parse exception", t);
    Xmpp.sendMessage(user, "Could not parse OPML file");
  }
 

}
