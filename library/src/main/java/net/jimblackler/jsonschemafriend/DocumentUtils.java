package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentUtils {
  public static Object loadJson(InputStream inputStream) throws IOException {
    return parseJson(streamToString(inputStream));
  }

  public static Object parseJson(String content) {
    content = content.replaceAll("[\uFEFF-\uFFFF]", ""); // Strip the dreaded FEFF.
    content = content.trim();
    char firstChar = content.charAt(0);
    if (firstChar == '[') {
      return new JSONArray(content);
    } else if (firstChar == '{') {
      return new JSONObject(content);
    } else {
      throw new JSONException("Doesn't look like JSON.");
    }
  }
}
