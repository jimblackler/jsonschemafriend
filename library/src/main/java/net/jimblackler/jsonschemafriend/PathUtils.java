package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONPointerException;

public class PathUtils {
  public static final String ESCAPED_EMPTY = "~2";
  private static final Logger LOG = Logger.getLogger(PathUtils.class.getName());

  public static URI append(URI uri, String value) {
    String uriString = uri.toString();
    if (!uriString.contains("#")) {
      uriString += "#";
    }

    if (uriString.charAt(uriString.length() - 1) != '/') {
      uriString += "/";
    }

    value = value.replace("~", "~0").replace("/", "~1");
    value = uriComponentEncode(value);
    if (value.isEmpty()) {
      value = ESCAPED_EMPTY;
    }

    return URI.create(uriString + value);
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

    // Special escape string for an empty key in a path.
    // Empty keys mid-path could be represented with // (nothing between the separators), but
    // that would not work for keys at the end of the path.
    path = path.replace(ESCAPED_EMPTY, "");
    JSONPointer jsonPointer = new JSONPointer("#" + path);
    try {
      return jsonPointer.queryFrom(document);
    } catch (JSONPointerException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static Object modifyAtPath(Object document, String path, Object newObject)
      throws MissingPathException {
    if (path == null || path.isEmpty()) {
      return newObject;
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
          return document;
        }
        if (parentObject instanceof JSONArray) {
          ((JSONArray) parentObject).put(Integer.parseInt(lastPart), newObject);
          return document;
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

  static String uriComponentEncode(String value) {
    String encoded = URLEncoder.encode(value);

    // $ is a common character in schema paths, and it doesn't strictly require escaping, so for
    // aesthetic reasons we don't escape it.
    encoded = encoded.replace("%24", "$");

    // Encoding tilde causes a conflict with the JSON Pointer encoding.
    encoded = encoded.replace("%7E", "~");

    return encoded;
  }

  private static String jsonPointerUnescape(String token) {
    // This matches the JSONPointer escaping method which may not match RFC 6901 which does not
    // require quotes to be encoded.
    return token.replace("~1", "/").replace("~0", "~").replace("\\\"", "\"").replace("\\\\", "\\");
  }

  public static URI getParent(URI uri) {
    String pointer = uri.getRawFragment();
    if (pointer == null) {
      return null;
    }
    int i = pointer.lastIndexOf("/");
    if (i == -1) {
      return null;
    }
    try {
      return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), pointer.substring(0, i));
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }

  static <T> URI resolve(URI base, URI child) {
    if ("jar".equals(base.getScheme())) {
      // Path.resolve() doesn't like to handle jar: form URLs - a problem if apps directly load
      // schemas from libraries that cross-reference each other - so we use a little hack.
      URI converted = URI.create(base.toString().substring("jar:".length()));
      URI resolved = resolve(converted, child);
      if ("file".equals(resolved.getScheme())) {
        return URI.create("jar:" + resolved);
      }
      // If the destination URI is not a file, it's not going to be in the jar.
      return resolved;
    }
    URI resolve = base.resolve(child);
    return normalize(resolve);
  }

  /**
   * Applications are supposed to escape $refs but they often don't. We help out applications by
   * attempting to escape some of these characters. Some we can never fix in this way, such as the
   * percent character or forward slash character.
   *
   * @param ref The URI to possibly fix.
   * @return The fixed ref.
   */
  public static String fixUnescaped(String ref) {
    int i = ref.indexOf("#");
    if (i == -1) {
      return ref;
    }

    String fragment = ref.substring(i + 1);
    String value = fragment;
    value = value.replace("\t", "%09");
    value = value.replace("\n", "%0A");
    value = value.replace("\f", "%0C");
    value = value.replace("\r", "%0D");
    value = value.replace("!", "%21");
    value = value.replace("\"", "%22");
    value = value.replace("#", "%23");
    value = value.replace("+", "%2B");
    value = value.replace(":", "%3A");
    value = value.replace("<", "%3C");
    value = value.replace(">", "%3E");
    value = value.replace("?", "%3F");
    value = value.replace("\\", "%5C");
    value = value.replace("^", "%5E");
    value = value.replace("`", "%60");
    value = value.replace("{", "%7B");
    value = value.replace("|", "%7C");
    value = value.replace("}", "%7D");
    if (value.equals(fragment)) {
      return ref;
    }
    String ref2 = ref.substring(0, i + 1) + value;
    LOG.warning(
        "Converting unescaped reference " + ref + " to JSON Schema legal $ref form " + ref2);
    return ref2;
  }

  /**
   * Convert a URI to its standard form, for the purposes of looking up URIs in internal
   * dictionaries. Where there are multiple valid ways to express the same URI we have to chose one
   * as the canonical form if URIs are to be used as keys in storage structures.
   * <p>
   * For example; http://example.com, http://example.com# and http://example.com#/ are all valid but
   * equivalent pointers.
   *
   * @param uri The URL to normalize.
   * @return The normalized form of the URI.
   */
  static URI normalize(URI uri) {
    String uriString = uri.toString();
    int length = uriString.length();
    if (uriString.endsWith("#")) {
      return URI.create(uriString.substring(0, length - "#".length()));
    }
    if (uriString.endsWith("#/")) {
      return URI.create(uriString.substring(0, length - "#/".length()));
    }
    return uri;
  }
}
