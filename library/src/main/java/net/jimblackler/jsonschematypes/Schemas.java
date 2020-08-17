package net.jimblackler.jsonschematypes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

public class Schemas {
  public static Schema create(SchemaContext schemaContext, Object object)
      throws GenerationException {
    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new TrivialSchema((boolean) object);
    }

    JSONObject jsonObject = (JSONObject) object;
    try {
      String ref = jsonObject.optString("$ref");
      if (ref.isEmpty()) {
        Object type = jsonObject.get("type");
        if (type instanceof JSONArray) {
          JSONArray array = (JSONArray) type;
          for (int idx = 0; idx != array.length(); idx++) {
            create2(schemaContext, jsonObject, array.getString(idx)); // treat as anyOf
          }
          throw new GenerationException();
        } else {
          return create2(schemaContext, jsonObject, type.toString());
        }
      } else {
        JSONPointer jsonPointer = new JSONPointer(ref);
        System.out.println(jsonPointer);

        return null;
      }
    } catch (JSONException e) {
      System.out.println(jsonObject.toString(2));
      throw new GenerationException(e);
    }
  }

  private static SchemaWithContext create2(SchemaContext schemaContext, JSONObject jsonObject,
      String simpleType) throws GenerationException {
    switch (simpleType) {
      case "array":
        return new ArraySchema(schemaContext, jsonObject);
      case "boolean":
        throw new GenerationException();
      case "integer":
        throw new GenerationException();
      case "null":
        throw new GenerationException();
      case "number":
        throw new GenerationException();
      case "object":
        return new ObjectSchema(schemaContext, jsonObject);
      case "string":
        return new StringSchema(schemaContext, jsonObject);
      default:
        throw new GenerationException("Unknown type " + simpleType);
    }
  }

  private static Schema createObject(JSONObject jsonObject) throws GenerationException {
    return null;
  }
}