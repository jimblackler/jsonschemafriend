package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_4;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_7;

import java.net.URI;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class MetaSchemaDetector {
  static URI detectMetaSchema(Object document) {
    if (document instanceof Boolean) {
      return DRAFT_7;
    }

    if (document instanceof JSONObject) {
      JSONObject jsonDocument = (JSONObject) document;
      if (jsonDocument.has("$schema")) {
        return URI.create(jsonDocument.getString("$schema"));
      }
      if (jsonDocument.has("schema")) {
        return URI.create(jsonDocument.getString("schema"));
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
      return DRAFT_7;
    }
    return DRAFT_4;
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
