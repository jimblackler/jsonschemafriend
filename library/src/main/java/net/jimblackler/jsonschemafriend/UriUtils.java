package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UriUtils {
  public static URI withoutFragment(URI uri) {
    String s = uri.toString();
    int i = s.indexOf('#');
    if (i == -1) {
      return uri;
    }
    return URI.create(s.substring(0, i));
  }
}
