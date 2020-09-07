package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import org.json.JSONArray;
import org.json.JSONObject;
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

    value = uriComponentEncode(value);

    return URI.create(baseDocumentFromUri(uri) + "#" + fragment + value);
  }

  static URI baseDocumentFromUri(URI path) {
    try {
      return new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Object fetchFromPath(Object document, String path) throws MissingPathException {
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
        throw new MissingPathException(ex);
      }
    } catch (IllegalArgumentException ex) {
      throw new MissingPathException("Probable attempt to use an $id as a URL", ex);
    }
  }

  public static void modifyAtPath(Object document, String path, Object newObject)
      throws MissingPathException {
    if (path == null || path.isEmpty()) {
      throw new MissingPathException();
    }
    try {
      String parentPath = getParentPath(path);
      path = path.replace(ESCAPED_EMPTY, "");
      JSONPointer jsonPointer = new JSONPointer("#" + parentPath);
      try {
        Object parentObject = jsonPointer.queryFrom(document);
        String lastPart = getLastPart(path);
        lastPart = URLDecoder.decode(lastPart);
        lastPart = jsonPointerUnescape(lastPart);

        if (parentObject instanceof JSONObject) {
          ((JSONObject) parentObject).put(lastPart, newObject);
          return;
        }
        if (parentObject instanceof JSONArray) {
          ((JSONArray) parentObject).put(Integer.parseInt(lastPart), newObject);
          return;
        }
        throw new MissingPathException("Could not modify document");
      } catch (JSONPointerException ex) {
        throw new MissingPathException(ex);
      }
    } catch (IllegalArgumentException ex) {
      throw new MissingPathException("Probable attempt to use an $id as a URL", ex);
    }
  }

  public static void deleteAtPath(Object document, String path) throws MissingPathException {
    if (path == null || path.isEmpty()) {
      throw new MissingPathException();
    }
    try {
      String parentPath = getParentPath(path);
      path = path.replace(ESCAPED_EMPTY, "");
      JSONPointer jsonPointer = new JSONPointer("#" + parentPath);
      try {
        Object parentObject = jsonPointer.queryFrom(document);
        String lastPart = getLastPart(path);
        lastPart = URLDecoder.decode(lastPart);
        lastPart = jsonPointerUnescape(lastPart);

        if (parentObject instanceof JSONObject) {
          ((JSONObject) parentObject).remove(lastPart);
          return;
        }
        if (parentObject instanceof JSONArray) {
          ((JSONArray) parentObject).remove(Integer.parseInt(lastPart));
          return;
        }
        throw new MissingPathException("Could not modify document");
      } catch (JSONPointerException ex) {
        throw new MissingPathException(ex);
      }
    } catch (IllegalArgumentException ex) {
      throw new MissingPathException("Probable attempt to use an $id as a URL", ex);
    }
  }

  private static String getParentPath(String path) throws MissingPathException {
    int i = path.lastIndexOf("/");
    if (i == -1) {
      throw new MissingPathException("No parent");
    }
    return path.substring(0, i);
  }

  private static String getLastPart(String path) {
    int i = path.lastIndexOf("/");
    if (i == -1) {
      return path;
    }
    return path.substring(i + 1);
  }

  private static String uriComponentEncode(String value) {
    String encoded = URLEncoder.encode(value);

    // $ is a common character in schema paths, and it doesn't strictly require escaping, so for
    // aesthetic reasons we don't escape it.
    encoded = encoded.replace("%24", "$");

    // Encoding tilde causes a conflict with the JSON Pointer encoding.
    encoded = encoded.replace("%7E", "~");

    return encoded;
  }

  private static String jsonPointerEscape(String token) {
    return token.replace("\\", "\\\\").replace("\"", "\\\"").replace("~", "~0").replace("/", "~1");
  }

  private static String jsonPointerUnescape(String token) {
    return token.replace("~1", "/").replace("~0", "~").replace("\\\"", "\"").replace("\\\\", "\\");
  }

  public static String refPathEscape(String value) {
    // These characters have been seen in paths.
    value = value.replace("<", "%3C");
    value = value.replace(">", "%3E");
    return value;
  }
}
