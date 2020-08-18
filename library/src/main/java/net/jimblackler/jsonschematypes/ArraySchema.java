package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ArraySchema implements Schema {
  private List<URI> arrayTypes = new ArrayList<>();
  private URI singleType;

  public ArraySchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    JSONObject jsonObject = (JSONObject) schemaStore.resolve(pointer);

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.get("items");
    if (items instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) items;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        arrayTypes.add(JsonSchemaRef.append(pointer, String.valueOf(idx)));
      }
    } else {
      singleType = schemaStore.require(JsonSchemaRef.append(pointer, "items"));
    }
  }
}
