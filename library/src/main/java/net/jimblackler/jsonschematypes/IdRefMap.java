package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

class IdRefMap {
  private final Map<URI, URI> idToPath = new HashMap<>();
  private final Map<URI, URI> refs = new HashMap<>();

  void map(SchemaStore schemaStore, URI path, URI activeId) throws GenerationException {
    Object object = schemaStore.getSchemaJson(path);
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Object idObject = jsonObject.opt("$id");
      if (idObject instanceof String) {
        URI newId = URI.create((String) idObject);
        if (activeId != null) {
          // See "This URI also serves as the base URI.." in
          // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-8.2.2
          newId = activeId.resolve(newId);
        }
        idToPath.put(newId, path);
        activeId = newId;
      }

      Object refObject = jsonObject.opt("$ref");
      if (refObject instanceof String) {
        String ref = (String) refObject;
        URI resolveWith = activeId == null || ref.startsWith("#") ? path : activeId;
        refs.put(path, resolveWith.resolve(URI.create(ref)));
      }

      for (String key : jsonObject.keySet()) {
        map(schemaStore, JsonSchemaRef.append(path, key), activeId);
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        map(schemaStore, JsonSchemaRef.append(path, String.valueOf(idx)), activeId);
      }
    }
  }

  public URI pathOrIdToPath(URI uri) {
    if (idToPath.containsKey(uri)) {
      return finalPath(idToPath.get(uri));
    }
    return uri;
  }

  /**
   * Converts a URI containing a path to the final path of the schema.
   *
   * @param path The id or path.
   * @return The final path of the schema.
   */
  public URI finalPath(URI path) {
    while (refs.containsKey(path)) {
      path = refs.get(path);
      // This can be an $id, in which case we must convert it back to a path.
      if (idToPath.containsKey(path)) {
        path = idToPath.get(path);
      }
    }
    return path;
  }
}
