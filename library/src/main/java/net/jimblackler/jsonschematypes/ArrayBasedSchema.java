package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class ArrayBasedSchema implements Schema {
  private final List<URI> indices = new ArrayList<>();

  ArrayBasedSchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    JSONArray resolve = (JSONArray) schemaStore.resolve(pointer);
    for (int idx = 0; idx != resolve.length(); idx++) {
      URI indexPointer = JsonSchemaRef.append(pointer, String.valueOf(idx));
      indices.add(schemaStore.require(indexPointer));
    }
  }
}
