package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

public class ArrayBasedSchema implements Schema {
  private final List<URI> indices = new ArrayList<>();

  ArrayBasedSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    JSONArray resolve = (JSONArray) schemaStore.resolve(uri);
    for (int idx = 0; idx != resolve.length(); idx++) {
      URI indexPointer = JsonSchemaRef.append(uri, String.valueOf(idx));
      indices.add(schemaStore.followAndQueue(indexPointer));
    }
  }
}
