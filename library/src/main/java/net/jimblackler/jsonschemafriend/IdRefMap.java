package net.jimblackler.jsonschemafriend;

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

  private static <T> URI resolve(URI base, URI child) {
    if ("jar".equals(base.getScheme())) {
      // Path.resolve() doesn't like to handle jar: form URLs - a problem if apps directly load
      // schemas from libraries that cross-reference each other - so we use a little hack.
      URI converted = URI.create(base.toString().substring("jar:".length()));
      URI resolved = resolve(converted, child);
      return URI.create("jar:" + resolved);
    }
    return base.resolve(child);
  }

  void map(JSONObject baseDocument, URI uri, URI activeId, JSONObject metaSchemaDocument) {
    JSONObject properties = metaSchemaDocument.optJSONObject("properties");
    String idKey = properties == null || properties.has("$id") ? "$id" : "id";

    Object object = PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Object idObject = jsonObject.opt(idKey);
      if (idObject instanceof String) {
        URI newId = URI.create((String) idObject);
        if (activeId != null) {
          // See "This URI also serves as the base URI.." in
          // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-8.2.2
          newId = resolve(activeId, newId);
        }
        idToPath.put(newId, uri);
        activeId = newId;
      }

      Object refObject = jsonObject.opt("$ref");
      if (refObject instanceof String) {
        String ref = PathUtils.refPathEscape((String) refObject);
        URI resolveWith = activeId == null || ref.startsWith("#") ? uri : activeId;
        URI resolve = resolve(resolveWith, URI.create(ref));
        refs.put(uri, resolve);
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
   * @param defaultMetaSchema The default meta-schema to use.
   * @return The final uri of the schema.
   */
  public URI finalLocation(URI uri, DocumentSource documentSource, URI defaultMetaSchema)
      throws GenerationException {
    while (true) {
      URI baseDocumentUri = PathUtils.baseDocumentFromUri(uri);
      if (mapped.add(baseDocumentUri)) {
        Object baseDocumentObject = documentSource.fetchDocument(baseDocumentUri);
        // The document can be a single boolean, and still hold a legal schema. There's nothing to
        // do in this case.
        if (baseDocumentObject instanceof Boolean) {
          return uri;
        }
        JSONObject baseDocument = (JSONObject) baseDocumentObject;
        URI metaSchemaUri = baseDocument.has("$schema")
            ? URI.create(baseDocument.getString("$schema"))
            : defaultMetaSchema;
        map(baseDocument, baseDocumentUri, baseDocumentUri, (JSONObject) documentSource.fetchDocument(metaSchemaUri));
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
