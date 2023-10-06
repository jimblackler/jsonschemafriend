package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MetaSchemaDetector {
  static URI detectMetaSchema(Object document) {
    if (document instanceof Boolean) {
      return MetaSchemaUris.DRAFT_7;
    }

    if (document instanceof Map) {
      Map<String, Object> jsonDocument = (Map<String, Object>) document;
      if (jsonDocument.containsKey("$schema")) {
        return URI.create((String) jsonDocument.get("$schema"));
      }
      if (jsonDocument.containsKey("schema")) {
        return URI.create((String) jsonDocument.get("schema"));
      }
    }

    if (document instanceof List) {
      List<Object> objects = (List<Object>) document;
      if (objects.size() > 0) {
        return detectMetaSchema(objects.get(0));
      }
      return MetaSchemaUris.DRAFT_2019_09;
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
      return MetaSchemaUris.DRAFT_7;
    }
    return MetaSchemaUris.DRAFT_4;
  }

  private static void allKeys(Object document, Consumer<String> consumer) {
    if (document instanceof Map) {
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) document).entrySet()) {
        consumer.accept(entry.getKey());
        allKeys(entry.getValue(), consumer);
      }
    } else if (document instanceof List) {
      for (Object o : (Iterable<Object>) document) {
        allKeys(o, consumer);
      }
    }
  }
}
