package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import info.persistent.pushbot.util.Urls;
import info.persistent.pushbot.util.Xmpp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class FeedCommandHandler implements CommandHandler {
  private static final Logger logger =
      Logger.getLogger(FeedCommandHandler.class.getName());

  @Override
  public void handle(JID user, String... args) {
    if (args.length != 1) {
      Xmpp.sendMessage(user, "feed URL not specified");
      return;
    }

    URL feedUrl;
    try {
      feedUrl = new URL(args[0]);
    } catch (MalformedURLException err) {
      logger.log(Level.INFO, "URL parse exception", err);
      Xmpp.sendMessage(user, "Feed URL is malformed");
      return;
    }

    feedUrl = normalizeFeedUrl(feedUrl);
    if (feedUrl == null) {
      Xmpp.sendMessage(user, "This does not appear to be a feed URL");
      return;
    }

    handle(user, feedUrl);
  }

  private static URL normalizeFeedUrl(URL feedUrl) {
    URL queryUrl;

    try {
      queryUrl =
          new URL(
              "http://ajax.googleapis.com/ajax/services/feed/lookup?v=1.0&q="
                  + Urls.encode(feedUrl.toString()));
    } catch (MalformedURLException err) {
      // We know exactly what's in this URL, it should well-formed
      throw new RuntimeException(err);
    }

    BufferedReader queryReader;
    try {
      queryReader =
          new BufferedReader(new InputStreamReader(queryUrl.openStream()));

    } catch (IOException err) {
      logger.log(Level.INFO, "Could not query AJAX Feed API", err);
      return null;
    }

    JSONObject queryJson = (JSONObject) JSONValue.parse(queryReader);
    Number responseStatus = (Number) queryJson.get("responseStatus");

    if (responseStatus == null || responseStatus.intValue() >= 300) {
      return null;
    }

    JSONObject responseData = (JSONObject) queryJson.get("responseData");
    if (responseData == null) {
      return null;
    }
    String responseUrl = (String) responseData.get("url");

    try {
      return new URL(responseUrl);
    } catch (MalformedURLException err) {
      logger.log(Level.INFO, "Malformed feed URL in response from API", err);
      return null;
    }
  }

  protected abstract void handle(JID user, URL feedUrl);

}
