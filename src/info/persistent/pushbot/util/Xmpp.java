package info.persistent.pushbot.util;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.SendResponse;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import java.util.logging.Logger;

public class Xmpp {
  private static final Logger logger = Logger.getLogger(Xmpp.class.getName());

  private Xmpp() {
    // Not instantiable
  }

  public static void sendMessage(JID toJid, String body) {
    logger.info("Sending message " + body + " to " + toJid);
    
    XMPPService xmpp = XMPPServiceFactory.getXMPPService();
    
    Message message =
        new MessageBuilder().withRecipientJids(toJid).withBody(body).build();
  
    SendResponse sendResponse = xmpp.sendMessage(message);
    SendResponse.Status sendStatus = sendResponse.getStatusMap().get(toJid); 
    
    if (sendStatus != SendResponse.Status.SUCCESS) {
      logger.warning("Send status to " + toJid + " was " + sendStatus);
    }
  }
  
  /** Strips the resource part of out of a JID, useful when persisting them. */
  public static JID toShortJid(JID jid) {
    return new JID(jid.getId().split("/")[0]);
  }
}
