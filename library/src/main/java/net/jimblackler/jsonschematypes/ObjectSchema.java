package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

public class ObjectSchema implements Schema {
  private final Map<String, URI> _properties = new HashMap<>();

  public ObjectSchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    JSONObject jsonObject = (JSONObject) schemaStore.resolve(pointer);
    { // Properties

      if (jsonObject.has("properties")) {
        JSONObject properties = jsonObject.getJSONObject("properties");
        URI propertiesPointer = JsonSchemaRef.append(pointer, "properties");
        Iterator<String> it = properties.keys();
        while (it.hasNext()) {
          String propertyName = it.next();
          _properties.put(propertyName,
              schemaStore.require(JsonSchemaRef.append(propertiesPointer, propertyName)));
        }
      }

      // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
      if (jsonObject.has("additionalProperties")) {
        schemaStore.require(JsonSchemaRef.append(pointer, "additionalProperties"));
        // We're not doing anything with this yet.
      }

      if (jsonObject.has("definitions")) {
        schemaStore.require(JsonSchemaRef.append(pointer, "definitions"));
        // We're not doing anything with this yet.
      }
    }
  }
}
