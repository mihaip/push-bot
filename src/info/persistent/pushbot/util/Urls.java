package info.persistent.pushbot.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Urls {
  public static String encode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is unlikely to be unsupported
      throw new RuntimeException(e);
    }
  }

}
