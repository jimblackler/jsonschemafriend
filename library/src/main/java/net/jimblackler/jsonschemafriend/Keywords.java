package net.jimblackler.jsonschemafriend;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Keywords {
  static final int SCHEMA = 1;
  static final int MAP_OF_SCHEMAS = 2;
  static final int LIST_OF_SCHEMAS = 4;

  static final Map<String, Integer> KEY_TYPES = getKeyTypes();

  /**
   * Returns a map of known keywords, and flags to indicate what can be stored there. This is used
   * during the mapping of the Schema JSON before building the Schema object, as only subschemas
   * under known keys can contain $id values used for referencing.
   *
   * The metaschema cannot be used to identify the known keys prior to mapping for a number of
   * reasons, including the fact that the metaschema would itself need to be mapped, creating a
   * circular dependency.
   *
   * A side benefit is that the user can be warned of unknown keywords in the schema.
   *
   * @return A map of known keywords and the expected types of associated data.
   */
  private static Map<String, Integer> getKeyTypes() {
    Map<String, Integer> keyTypes = new HashMap<>();
    keyTypes.put("$anchor", 0);
    keyTypes.put("$comment", 0);
    keyTypes.put("$defs", MAP_OF_SCHEMAS);
    keyTypes.put("$id", 0);
    keyTypes.put("$recursiveAnchor", 0);
    keyTypes.put("$recursiveRef", 0);
    keyTypes.put("$ref", 0);
    keyTypes.put("$schema", 0);
    keyTypes.put("$vocabulary", 0);
    keyTypes.put("additionalItems", SCHEMA);
    keyTypes.put("additionalProperties", SCHEMA);
    keyTypes.put("allOf", LIST_OF_SCHEMAS);
    keyTypes.put("anyOf", LIST_OF_SCHEMAS);
    keyTypes.put("const", 0);
    keyTypes.put("contains", SCHEMA);
    keyTypes.put("contentEncoding", 0);
    keyTypes.put("contentMediaType", 0);
    keyTypes.put("default", 0);
    keyTypes.put("definitions", MAP_OF_SCHEMAS);
    keyTypes.put("dependencies", MAP_OF_SCHEMAS | LIST_OF_SCHEMAS);
    keyTypes.put("dependentRequired", 0);
    keyTypes.put("dependentSchemas", MAP_OF_SCHEMAS);
    keyTypes.put("deprecated", 0);
    keyTypes.put("description", 0);
    keyTypes.put("disallow", 0);
    keyTypes.put("divisibleBy", 0);
    keyTypes.put("else", MAP_OF_SCHEMAS);
    keyTypes.put("enum", 0);
    keyTypes.put("examples", 0);
    keyTypes.put("exclusiveMaximum", 0);
    keyTypes.put("exclusiveMinimum", 0);
    keyTypes.put("extends", MAP_OF_SCHEMAS);
    keyTypes.put("format", 0);
    keyTypes.put("id", 0);
    keyTypes.put("if", SCHEMA);
    keyTypes.put("items", SCHEMA | LIST_OF_SCHEMAS);
    keyTypes.put("maxContains", 0);
    keyTypes.put("maxItems", 0);
    keyTypes.put("maxLength", 0);
    keyTypes.put("maxProperties", 0);
    keyTypes.put("maximum", 0);
    keyTypes.put("minContains", 0);
    keyTypes.put("minItems", 0);
    keyTypes.put("minLength", 0);
    keyTypes.put("minProperties", 0);
    keyTypes.put("minimum", 0);
    keyTypes.put("multipleOf", 0);
    keyTypes.put("not", SCHEMA);
    keyTypes.put("oneOf", LIST_OF_SCHEMAS);
    keyTypes.put("pattern", 0);
    keyTypes.put("patternProperties", MAP_OF_SCHEMAS);
    keyTypes.put("prefixItems", MAP_OF_SCHEMAS);
    keyTypes.put("properties", MAP_OF_SCHEMAS);
    keyTypes.put("propertyNames", SCHEMA);
    keyTypes.put("required", 0);
    keyTypes.put("then", SCHEMA);
    keyTypes.put("title", 0);
    keyTypes.put("type", 0);
    keyTypes.put("unevaluatedItems", SCHEMA);
    keyTypes.put("unevaluatedProperties", SCHEMA);
    keyTypes.put("uniqueItems", 0);
    return Collections.unmodifiableMap(keyTypes);
  }

  public static void get(String key) {}
}
