package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Schemas {
  public static Schema create(SchemaStore schemaStore, URI uri) throws GenerationException {
    Object object = schemaStore.resolve(uri);
    if (object == null) {
      throw new GenerationException("Cannot follow " + uri);
    }
    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new TrivialSchema((boolean) object);
    }

    JSONObject jsonObject = (JSONObject) object;

    try {
      if (jsonObject.has("allOf")) {
        return new AllOfSchema(schemaStore, JsonSchemaRef.append(uri, "allOf"));
      }

      if (jsonObject.has("anyOf")) {
        return new AnyOfSchema(schemaStore, JsonSchemaRef.append(uri, "anyOf"));
      }

      if (jsonObject.has("oneOf")) {
        return new OneOfSchema(schemaStore, JsonSchemaRef.append(uri, "oneOf"));
      }

      Object type = jsonObject.opt("type");
      if (type == null) {
        return new TrivialSchema(false);
      }
      Set<String> types = new HashSet<>();
      if (type instanceof JSONArray) {
        JSONArray array = (JSONArray) type;
        for (int idx = 0; idx != array.length(); idx++) {
          types.add(array.getString(idx));
        }
      } else {
        types.add(type.toString());
      }
      if (types.isEmpty()) {
        throw new GenerationException("No types");
      }

      return new MultiplePrimitiveSchema(schemaStore, uri, types);

    } catch (JSONException e) {
      System.out.println(uri);
      System.out.println(jsonObject.toString(2));
      throw new GenerationException(e);
    }
  }
}