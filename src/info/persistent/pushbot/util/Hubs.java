package info.persistent.pushbot.util;

import com.google.appengine.api.xmpp.JID;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Hubs {
  private static final Logger logger = Logger.getLogger(Hubs.class.getName());
  
  private Hubs() {
    // Not instantiable
  }

  public static void sendRequestToHub(
      JID user,
      URL hubUrl,
      URL feedUrl,
      boolean isSubscribe) {
    String verb = isSubscribe ? "subscribe" : "unsubscribe";
    logger.info(
        "Sending " + verb + " request to hub " + hubUrl + " for " + feedUrl);
    try {
      HttpURLConnection connection =
          (HttpURLConnection) hubUrl.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
  
      OutputStreamWriter writer =
          new OutputStreamWriter(connection.getOutputStream());
      writeHubRequestParams(user, feedUrl, verb, writer);
      writer.close();
  
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_NO_CONTENT
          || responseCode == HttpURLConnection.HTTP_ACCEPTED) {
        // Nothing to do in this case
        logger.info("Successfully sent " + verb + " request to " + hubUrl);
      } else {
        logger.info("Could not make " + verb + " request to hub " + hubUrl +
            " " + responseCode + "/" + connection.getResponseMessage());
        StringWriter debugWriter = new StringWriter();
        writeHubRequestParams(user, feedUrl, verb, debugWriter);
        logger.info("Request params: " + debugWriter.toString());        
        Xmpp.sendMessage(user, "Could not " + verb + " at hub " + hubUrl);
      }
  
    } catch (IOException err) {
      logger.log(Level.INFO, "Could not " + verb + " to hub " + hubUrl, err);
      Xmpp.sendMessage(user, "Could not " + verb + " at hub " + hubUrl);
    }
  }

  private static void writeHubRequestParams(
      JID user, URL feedUrl, String verb, Writer writer) throws IOException {
    String callbackUrl =
        "http://push-bot.appspot.com/push-subscriber/"
            + Urls.encode(Xmpp.toShortJid(user).getId());
    writer.write("hub.callback=" + Urls.encode(callbackUrl));
    writer.write("&hub.mode=" + verb);
    writer.write("&hub.topic=" + Urls.encode(feedUrl.toString()));
    writer.write("&hub.verify=sync");
    // TODO(mihaip): Actually use a verification token.
    writer.write("&hub.verify_token=");
  }
}
