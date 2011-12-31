// Copyright 2011 Google Inc. All Rights Reserved.

package info.persistent.pushbot;

import com.google.appengine.api.utils.HttpRequestParser;
import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.data.XmppError;
import info.persistent.pushbot.util.Persistence;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class XmppErrorServlet extends HttpServlet {
  // TODO(mihaip): switch to Error and InboundErrorParser once they're available.
  private class Error {
    private final JID from;
    private final JID to;
    private final String body;
    private final String stanza;

    public Error(JID from, JID to, String body, String stanza) {
      this.from = from;
      this.to = to;
      this.body = body;
      this.stanza = stanza;
    }
    
    @Override public String toString() {
      String response = "";
      if (from != null) {
        response += "from: " + from + "\n";
      }
      if (to != null) {
        response += "to: " + to + "\n";
      }
      if (body != null) {
        response += "body: " + body + "\n";
      }
      if (stanza != null) {
        response += "stanza: " + stanza + "\n";
      }
      return response;

    }
  }
  
  private class ErrorParser extends HttpRequestParser {    
    private Error parse(HttpServletRequest req) throws IOException {
      try {
        MimeMultipart multipart = parseMultipartRequest(req);
      
        JID from = null;
        JID to = null;
        String body = null;
        String stanza = null;
        
        for (int i = 0; i < multipart.getCount(); i++) {
          BodyPart part = multipart.getBodyPart(i);
          String fieldName = getFieldName(part);
          if ("from".equals(fieldName)) {
            from = new JID(getTextContent(part));
          } else if ("to".equals(fieldName)) {
            to = new JID(getTextContent(part));
          } else if ("body".equals(fieldName)) {
            body = getTextContent(part);
          } else if ("stanza".equals(fieldName)) {
            stanza = getTextContent(part);
          }
        }
        
        return new Error(from, to, body, stanza);
      } catch (MessagingException err) {
        logger.log(Level.WARNING, "Could not parse error request", err);
        return null;
      }
    }
  }
  
  public static final Logger logger =
      Logger.getLogger(XmppErrorServlet.class.getName());

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    Error error = new ErrorParser().parse(req);
    if (error != null) {
      logger.info("XMPP error: " + error);
      if (error.from != null) {
        final XmppError xmppError = XmppError.getOrCreateForUser(error.from);
        xmppError.incrementErrorCount();
        logger.info(error.from.getId() + " now has " + xmppError.getErrorCount() + " errors");
        Persistence.withManager(new Persistence.Closure() {
          @Override
          public void run(PersistenceManager manager) {
            manager.makePersistent(xmppError);
          }
        });
      }
      resp.getWriter().write("OK");
    } else {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
