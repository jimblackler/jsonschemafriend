package net.jimblackler.jsonschematypes;

import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Utils {
  static Object getJsonObject(String filename) throws GenerationException {
    try (Scanner scanner = new Scanner(Test.class.getResourceAsStream(filename))) {
      String jsonString = scanner.useDelimiter("\\A").next();
      try {
        return new JSONArray(jsonString);
      } catch (JSONException e) {
        // Ignored by default.
      }
      try {
        return new JSONObject(jsonString);
      } catch (JSONException e) {
        System.out.println(jsonString);
        throw new GenerationException(e);
      }
    }
  }
}