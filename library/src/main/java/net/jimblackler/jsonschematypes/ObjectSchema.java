package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import org.json.JSONObject;

public class ObjectSchema implements Schema {
  private final Map<String, URI> _properties = new HashMap<>();

  public ObjectSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    JSONObject jsonObject = (JSONObject) schemaStore.resolve(uri);
    { // Properties

      if (jsonObject.has("properties")) {
        JSONObject properties = jsonObject.getJSONObject("properties");
        URI propertiesPointer = JsonSchemaRef.append(uri, "properties");
        Iterator<String> it = properties.keys();
        while (it.hasNext()) {
          String propertyName = it.next();
          _properties.put(propertyName,
              schemaStore.followAndQueue(JsonSchemaRef.append(propertiesPointer, propertyName)));
        }
      }

      // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
      if (jsonObject.has("additionalProperties")) {
        schemaStore.followAndQueue(JsonSchemaRef.append(uri, "additionalProperties"));
        // We're not doing anything with this yet.
      }

      if (jsonObject.has("definitions")) {
        schemaStore.followAndQueue(JsonSchemaRef.append(uri, "definitions"));
        // We're not doing anything with this yet.
      }
    }
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
