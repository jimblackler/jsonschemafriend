package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

class IdRefMap {
  private final Map<URI, URI> idToPath = new HashMap<>();
  private final Map<URI, URI> refs = new HashMap<>();
  private final Collection<URI> mapped = new HashSet<>();

  void map(DocumentSource documentSource, URI uri, URI activeId) throws GenerationException {
    Object document = documentSource.fetchDocument(PathUtils.baseDocumentFromUri(uri));
    if (document instanceof Boolean) {
      // The document can be a single boolean, and still hold a legal schema. There's noting to
      // map in this case.
      return;
    }

    Object object = PathUtils.fetchFromPath(document, uri.getRawFragment());

    // A meta-schema is required to understand how to navigate the document.
    URI metaSchema =
        URI.create(((JSONObject) document).optString("$schema", SchemaUtils.DEFAULT_SCHEMA));
    JSONObject metaSchemaDocument = (JSONObject) documentSource.fetchDocument(metaSchema);
    String idKey = metaSchemaDocument.getJSONObject("properties").has("$id") ? "$id" : "id";

    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Object idObject = jsonObject.opt(idKey);
      if (idObject instanceof String) {
        URI newId = URI.create((String) idObject);
        if (activeId != null) {
          // See "This URI also serves as the base URI.." in
          // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-8.2.2
          newId = activeId.resolve(newId);
        }
        idToPath.put(newId, uri);
        activeId = newId;
      }

      Object refObject = jsonObject.opt("$ref");
      if (refObject instanceof String) {
        String ref = (String) refObject;
        URI resolveWith = activeId == null || ref.startsWith("#") ? uri : activeId;
        refs.put(uri, resolveWith.resolve(URI.create(ref)));
      }

      for (String key : jsonObject.keySet()) {
        map(documentSource, PathUtils.append(uri, key), activeId);
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        map(documentSource, PathUtils.append(uri, String.valueOf(idx)), activeId);
      }
    }
  }

  /**
   * Converts a URI containing a path to the final uri of the schema.
   *
   * @param uri           The id or uri.
   * @param documentSource DocumentSource required to fetch any new documents found in the $refs.
   * @return The final uri of the schema.
   */
  public URI finalLocation(URI uri, DocumentSource documentSource) throws GenerationException {
    while (true) {
      URI document = PathUtils.baseDocumentFromUri(uri);
      if (mapped.add(document)) {
        map(documentSource, uri, uri);
      }
      if (!refs.containsKey(uri)) {
        return uri;
      }
      uri = refs.get(uri);
      // This can be an $id, in which case we must convert it back to a path.
      if (idToPath.containsKey(uri)) {
        uri = idToPath.get(uri);
      }
    }
  }
}
