package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class SchemaDetector {
  static URI detectSchema(Object document) {
    if (document instanceof JSONObject) {
      JSONObject jsonDocument = (JSONObject) document;
      if (jsonDocument.has("$schema")) {
        return URI.create(jsonDocument.getString("$schema"));
      }
    }
    int[] idCount = {0};
    int[] dollarIdCount = {0};
    allKeys(document, key -> {
      if ("id".equals(key)) {
        idCount[0]++;
      } else if ("$id".equals(key)) {
        dollarIdCount[0]++;
      }
    });

    if (dollarIdCount[0] > idCount[0]) {
      return URI.create("https://json-schema.org/draft-07/schema");
    }
    return URI.create("https://json-schema.org/draft-04/schema");
  }

  private static void allKeys(Object document, Consumer<String> consumer) {
    if (document instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) document;
      for (String key : jsonObject.keySet()) {
        consumer.accept(key);
        Object value = jsonObject.opt(key);
        allKeys(value, consumer);
      }
    } else if (document instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) document;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        allKeys(jsonArray.get(idx), consumer);
      }
    }
  }
}
