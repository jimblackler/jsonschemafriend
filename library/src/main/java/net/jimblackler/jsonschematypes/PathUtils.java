package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONPointer;

public class PathUtils {
  public static URI append(URI uri, String value) {
    try {
      String fragment = uri.getFragment();
      if (fragment == null) {
        fragment = "";
      }
      if (!fragment.endsWith("/")) {
        fragment += "/";
      }
      // See https://tools.ietf.org/html/rfc6901#section-3
      String escaped = value.replace("/", "~1").replace("~", "~1");
      return new URI(uri.getScheme(), uri.getSchemeSpecificPart(), fragment + escaped);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  static Object objectAtPath(Object document, URI path) {
    String query = path.getQuery();
    if (query != null && !query.isEmpty()) {
      // Query part can carry a string for validation while preserving the rest of the URI for error
      // messages. This is used for propertyName validation where it's not possible to link to the
      // name with a standard JSON Pointer.
      return query;
    }
    if (path.getFragment() == null) {
      return document;
    }
    return new JSONPointer("#" + path.getRawFragment()).queryFrom(document);
  }
}
