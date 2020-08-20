package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class PrimitiveSchema extends Schema {
  private final Map<String, Schema> _properties = new HashMap<>();
  private final List<Schema> arrayTypes = new ArrayList<>();
  private final Schema singleType;
  private final List<Schema> allOf;
  private final List<Schema> anyOf;
  private final List<Schema> oneOf;
  private final Set<String> explicitTypes = new HashSet<>();

  public PrimitiveSchema(SchemaStore schemaStore, URI path) throws GenerationException {
    super(schemaStore, path);
    JSONObject jsonObject = (JSONObject) schemaStore.resolvePath(path);

    Object type = jsonObject.opt("type");

    if (type instanceof JSONArray) {
      JSONArray array = (JSONArray) type;
      for (int idx = 0; idx != array.length(); idx++) {
        explicitTypes.add(array.getString(idx));
      }
    } else if (type instanceof String) {
      explicitTypes.add(type.toString());
    }

    if (jsonObject.has("properties")) {
      JSONObject properties = jsonObject.getJSONObject("properties");
      URI propertiesPointer = JsonSchemaRef.append(path, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        _properties.put(
            propertyName, schemaStore.build(JsonSchemaRef.append(propertiesPointer, propertyName)));
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
    if (jsonObject.has("additionalProperties")) {
      schemaStore.build(JsonSchemaRef.append(path, "additionalProperties"));
      // We're not doing anything with this yet.
    }

    if (jsonObject.has("definitions")) {
      schemaStore.build(JsonSchemaRef.append(path, "definitions"));
      // We're not doing anything with this yet.
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.opt("items");
    URI itemsPath = JsonSchemaRef.append(path, "items");
    if (items instanceof JSONObject || items instanceof Boolean) {
      singleType = schemaStore.build(itemsPath);
    } else {
      singleType = null;
      if (items instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray) items;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          arrayTypes.add(schemaStore.build(JsonSchemaRef.append(itemsPath, String.valueOf(idx))));
        }
      }
    }

    if (jsonObject.has("allOf")) {
      allOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("allOf");
      URI arrayPath = JsonSchemaRef.append(path, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = JsonSchemaRef.append(arrayPath, String.valueOf(idx));
        allOf.add(schemaStore.build(indexPointer));
      }
    } else {
      allOf = null;
    }

    if (jsonObject.has("anyOf")) {
      anyOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("anyOf");
      URI arrayPath = JsonSchemaRef.append(path, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = JsonSchemaRef.append(arrayPath, String.valueOf(idx));
        anyOf.add(schemaStore.build(indexPointer));
      }
    } else {
      anyOf = null;
    }

    if (jsonObject.has("oneOf")) {
      oneOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("oneOf");
      URI arrayPath = JsonSchemaRef.append(path, "oneOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = JsonSchemaRef.append(arrayPath, String.valueOf(idx));
        oneOf.add(schemaStore.build(indexPointer));
      }
    } else {
      oneOf = null;
    }
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {
    if (explicitTypes.contains("integer")) {
      if (!(jsonObject instanceof Integer)) {
        errorConsumer.accept(new ValidationError("Not an integer"));
      }
    }
  }
}
