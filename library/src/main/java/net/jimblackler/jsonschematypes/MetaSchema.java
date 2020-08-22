package net.jimblackler.jsonschematypes;

public class MetaSchema {
  public static int getNumber(String schema) {
    if (schema.contains("//json-schema.org/draft-03/schema")) {
      return 3;
    }
    if (schema.contains("//json-schema.org/draft-04/schema")) {
      return 4;
    }
    if (schema.contains("//json-schema.org/draft-06/schema")) {
      return 6;
    }
    if (schema.contains("//json-schema.org/draft-07/schema")) {
      return 7;
    }
    if (schema.contains("//json-schema.org/draft/2019-09/schema")) {
      return 8;
    }
    return Integer.MAX_VALUE;
  }
}
