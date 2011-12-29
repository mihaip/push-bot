package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

public interface CommandHandler {
  void handle(JID user, String... args);
}
