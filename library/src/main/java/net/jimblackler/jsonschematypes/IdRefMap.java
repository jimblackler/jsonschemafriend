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

  private void map(DocumentSource documentSource, URI uri, URI defaultMetaSchema)
      throws GenerationException {
    URI baseDocumentUri = PathUtils.baseDocumentFromUri(uri);
    Object baseDocumentObject = documentSource.fetchDocument(baseDocumentUri);
    if (baseDocumentObject instanceof Boolean) {
      // The document can be a single boolean, and still hold a legal schema. There's noting to
      // map in this case.
      return;
    }
    JSONObject baseDocument = (JSONObject) baseDocumentObject;

    URI metaSchemaUri;
    if (baseDocument.has("$schema")) {
      metaSchemaUri = URI.create(baseDocument.getString("$schema"));
    } else {
      metaSchemaUri = defaultMetaSchema;
    }

    // A meta-schema is required to understand how to navigate the document.
    JSONObject metaSchemaDocument = (JSONObject) documentSource.fetchDocument(metaSchemaUri);
    map(baseDocument, uri, uri, metaSchemaDocument);
  }

  void map(JSONObject baseDocument, URI uri, URI activeId, JSONObject metaSchemaDocument) {
    String idKey = metaSchemaDocument.getJSONObject("properties").has("$id") ? "$id" : "id";

    Object object = PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());
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
        map(baseDocument, PathUtils.append(uri, key), activeId, metaSchemaDocument);
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        map(baseDocument, PathUtils.append(uri, String.valueOf(idx)), activeId, metaSchemaDocument);
      }
    }
  }

  /**
   * Converts a URI containing a path to the final uri of the schema.
   *
   * @param uri               The id or uri.
   * @param documentSource    DocumentSource required to fetch any new documents found in the $refs.
   * @param defaultMetaSchema
   * @return The final uri of the schema.
   */
  public URI finalLocation(URI uri, DocumentSource documentSource, URI defaultMetaSchema)
      throws GenerationException {
    while (true) {
      URI document = PathUtils.baseDocumentFromUri(uri);
      if (mapped.add(document)) {
        map(documentSource, uri, defaultMetaSchema);
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
