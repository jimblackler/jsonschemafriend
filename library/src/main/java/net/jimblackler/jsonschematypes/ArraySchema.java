package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArraySchema implements Schema {
  private final List<URI> arrayTypes = new ArrayList<>();
  private URI singleType;

  public ArraySchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    JSONObject jsonObject = (JSONObject) schemaStore.resolve(uri);

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.opt("items");
    if (items instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) items;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        arrayTypes.add(JsonSchemaRef.append(uri, String.valueOf(idx)));
      }
    } else if (items instanceof JSONObject || items instanceof Boolean) {
      singleType = schemaStore.followAndQueue(JsonSchemaRef.append(uri, "items"));
    }
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
