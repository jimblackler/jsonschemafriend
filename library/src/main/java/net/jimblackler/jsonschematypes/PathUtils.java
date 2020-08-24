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

  static URI baseDocumentFromUri(URI path) {
    try {
      return new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Object fetchFromPath(Object document, String path) {
    if (path == null || path.isEmpty()) {
      return document;
    }
    try {
      return new JSONPointer("#" + path).queryFrom(document);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Probable attempt to use an $id as a URL", ex);
    }
  }
}
