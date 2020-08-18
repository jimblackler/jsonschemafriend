package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.net.URISyntaxException;

public class JsonSchemaRef {
  public static URI append(URI uri, String value) {
    try {
      String fragment = uri.getFragment();
      if (fragment == null) {
        fragment = "";
      }
      if (!fragment.endsWith("/")) {
        fragment += "/";
      }
      // TODO: implement escaping in https://tools.ietf.org/html/rfc6901#section-3
      return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(),
          uri.getPath(), uri.getQuery(), fragment + value);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
