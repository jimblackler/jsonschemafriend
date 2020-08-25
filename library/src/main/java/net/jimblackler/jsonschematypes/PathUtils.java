package net.jimblackler.jsonschematypes;

import org.json.JSONPointer;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

public class PathUtils {
  public static URI append(URI uri, String value) {
    String fragment = uri.getRawFragment();
    if (fragment == null) {
      fragment = "";
    }
    if (!fragment.endsWith("/")) {
      fragment += "/";
    }

    value = escape(value);

    try {
      value = URLEncoder.encode(value, "utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }

    return URI.create(baseDocumentFromUri(uri) + "#" + fragment + value);
  }

  private static String escape(String token) {
    return token.replace("~", "~0")
        .replace("/", "~1")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");
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
      JSONPointer jsonPointer = new JSONPointer("#" + path);
      Object object = jsonPointer.queryFrom(document);
      if (object == null) {
        throw new IllegalStateException("Could not fetch " + path);
      }
      return object;
    } catch (IllegalArgumentException ex) {
      throw new IllegalStateException("Probable attempt to use an $id as a URL", ex);
    }
  }
}
