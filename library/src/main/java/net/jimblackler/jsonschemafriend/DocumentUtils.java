package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import net.jimblackler.usejson.Json5Parser;
import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentUtils {
  public static Object loadJson(InputStream inputStream) throws IOException {
    return parseJson(streamToString(inputStream));
  }

  public static Object parseJson(String content) {
    return JSONObject.wrap(new Json5Parser().parse(content));
  }

  public static String toString(Object object) {
    if (object instanceof JSONObject) {
      return ((JSONObject) object).toString(2);
    }
    if (object instanceof JSONArray) {
      return ((JSONArray) object).toString(2);
    }
    return object.toString();
  }
}
