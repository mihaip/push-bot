package info.persistent.pushbot.commands;

public enum Command {
  SUBSCRIBE(
      "subscribe",
      " _URL_",
      "Start receivings updates about the given URL (feed URLs or " +
      "auto-discovery-enabled HTML URLs are supported).",
      new SubscribeCommandHandler()),
  UNSUBSCRIBE(
      "unsubscribe",
      " _URL_",
      "Stop receiving updates about the given feed URL.",
      new UnsubscribeCommandHandler()),
  UNSUBSCRIBE_ALL(
      "unsubscribe-all",
      "",
      "Stop receiving updates for all subscribed feeds.",
      new UnsubscribeAllCommandHandler()),
  JOIN_PARTYCHAT(
      "join-partychat",
      " _name_",
      "Join the given Partychat channel.",
      new JoinPartychatCommandHandler()),
  LIST_SUBSCRIPTIONS(
      "list-subscriptions",
      "",
      "Lists the current subscriptions.",
      new ListSubscriptionsCommandHandler()),
  OPML_IMPORT(
      "opml-import",
      " _URL_",
      "Subscribe to all of the feed URLs in the given OPML file.",
      new OpmlImportCommandHandler()),
  HELP("help", "", "This message", new HelpCommandHandler()),
  ADMIN_UNSUBSCRIBE(
      "admin-unsubscribe",
      " _JID_ _URL_",
      "Remove the given user's subscription.",
      new AdminUnsubscribeCommandHandler()),
  ADMIN_UNSUBSCRIBE_ALL(
      "admin-unsubscribe-all",
      " _JID_",
      "Remove all of the given user's subscriptions.",
      new AdminUnsubscribeAllCommandHandler());  
  private final String name;
  private final String argSample;
  private final String description;
  private final CommandHandler handler;
  
  private Command(
      String name,
      String argSample,
      String description,
      CommandHandler handler) {
    this.name = name;
    this.argSample = argSample;
    this.description = description;
    this.handler = handler;
  }
  
  public String getName() {
    return name;
  }
  
  public String getArgSample() {
    return argSample;
  }
  
  public String getDescription() {
    return description;
  }
  
  public CommandHandler getHandler() {
    return handler;
  }
}
