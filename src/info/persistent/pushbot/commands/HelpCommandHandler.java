package info.persistent.pushbot.commands;

import com.google.appengine.api.xmpp.JID;

import info.persistent.pushbot.util.Xmpp;

public class HelpCommandHandler implements CommandHandler {

  @Override
  public void handle(JID user, String... args) {
    StringBuilder message = new StringBuilder();

    message
        .append("Use PuSH Bot to subscribe and be notified of "
            + "PubSubHubbub-enabled feed updates in realtime. Possible commands:");

    for (Command command : Command.values()) {
      if (command.getHandler() instanceof AdminCommandHandler) {
        continue;
      }
      message.append("\n  /" + command.getName() + command.getArgSample() + ": "
          + command.getDescription());
    }

    Xmpp.sendMessage(user, message.toString());

  }

}
