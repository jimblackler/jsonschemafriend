package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentUtils {
  public static Object loadJson(InputStream inputStream) throws IOException {
    String content = streamToString(inputStream);
    return parseJson(content);
  }

  public static String streamToString(InputStream inputStream) throws IOException {
    try (InputStreamReader inputStreamReader =
             new InputStreamReader(inputStream, StandardCharsets.UTF_8);
         BufferedReader reader = new BufferedReader(inputStreamReader)) {
      String newline = System.lineSeparator();
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (output.length() > 0) {
          output.append(newline);
        }
        output.append(line);
      }
      return output.toString();
    }
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
      throw new IllegalStateException("Doesn't look like JSON.");
    }
  }
}
