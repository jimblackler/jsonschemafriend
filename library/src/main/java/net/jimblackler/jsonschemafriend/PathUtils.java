package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import org.json.JSONPointer;
import org.json.JSONPointerException;

public class PathUtils {
  public static final String ESCAPED_EMPTY = "~2";

  public static URI append(URI uri, String value) {
    String fragment = uri.getRawFragment();
    if (fragment == null) {
      fragment = "";
    }
    if (!fragment.endsWith("/")) {
      fragment += "/";
    }

    value = jsonPointerEscape(value);

    if (value.isEmpty()) {
      value = ESCAPED_EMPTY;
    }

    value = uriComponentEscape(value);

    return URI.create(baseDocumentFromUri(uri) + "#" + fragment + value);
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
      // Special escape string for an empty key in a path.
      // Empty keys mid-path could be represented with // (nothing between the separators), but
      // that would not work for keys at the end of the path.
      path = path.replace(ESCAPED_EMPTY, "");
      JSONPointer jsonPointer = new JSONPointer("#" + path);
      try {
        return jsonPointer.queryFrom(document);
      } catch (JSONPointerException ex) {
        return null;
      }
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("Probable attempt to use an $id as a URL", ex);
    }
  }

  private static String uriComponentEscape(String value) {
    String encoded = URLEncoder.encode(value);

    // $ is a common character in schema paths, and it doesn't strictly require escaping, so for
    // aesthetic reasons we don't escape it.
    encoded = encoded.replace("%24", "$");

    // Encoding tilde causes a conflict with the JSON Pointer encoding.
    encoded = encoded.replace("%7E", "~");

    return encoded;
  }

  public static String refPathEscape(String value) {
    // These characters have been seen in paths.
    value = value.replace("<", "%3C");
    value = value.replace(">", "%3E");
    return value;
  }

  private static String jsonPointerEscape(String token) {
    return token.replace("~", "~0").replace("/", "~1").replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
