package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.util.Xmpp;

public class JoinPartychatCommandHandler implements CommandHandler {

  @Override
  public void handle(JID user, String... args) {
    if (args.length != -1) {
      Xmpp.sendMessage(user, "Must specify a partychat name to join");
    }
    
    Xmpp.sendMessage(
        new JID(args[0]),
        "Joining at the request of " + Xmpp.toShortJid(user).getId());      
  }

}
