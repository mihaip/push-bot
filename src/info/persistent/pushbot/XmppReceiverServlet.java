package info.persistent.pushbot;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

import info.persistent.pushbot.commands.Command;
import info.persistent.pushbot.util.Xmpp;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class XmppReceiverServlet extends HttpServlet {
  private static Pattern PARTYCHAT_SENDER_RE =
      Pattern.compile("^\\s*\\[\"[^\"]+\"\\]\\s*");
  private static Pattern PARTYCHAT_PREFIX_RE =
    Pattern.compile("^\\s*push-?bot:\\s*");

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    XMPPService xmpp = XMPPServiceFactory.getXMPPService();
    Message message;
    
    try {
      message = xmpp.parseMessage(req);
    } catch (IllegalArgumentException err) {
      // These exceptions are apparently caused by a bug in the Google Talk 
      // Flash gadget, so let's just ignore them.
      // http://code.google.com/p/googleappengine/issues/detail?id=2082
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    JID fromJid = message.getFromJid();
    String body = message.getBody().trim();
    
    // If we're getting messages from Partychat, require them to be addressed
    // specifically to us
    Matcher partychatMatcher = PARTYCHAT_SENDER_RE.matcher(body); 
    if (partychatMatcher.find()) {
      body = partychatMatcher.replaceFirst("");
      partychatMatcher = PARTYCHAT_PREFIX_RE.matcher(body);
      if (!partychatMatcher.find()) {
        return;
      }
      body = partychatMatcher.replaceFirst("");
    }
    
    String[] bodyPieces = body.split("\\s+", 2);

    String commandName = bodyPieces[0];
    String[] args = Arrays.copyOfRange(bodyPieces, 1, bodyPieces.length);
    
    // Ignore non-commands
    if (commandName.charAt(0) != '/') {
      return;
    }
    commandName = commandName.substring(1);

    for (Command command : Command.values()) {
      if (command.getName().equals(commandName)) {
        command.getHandler().handle(fromJid, args);
        return;
      }
    }
    
    Xmpp.sendMessage(
        fromJid, "Unknown command, see /help for more information.");
  }
}
