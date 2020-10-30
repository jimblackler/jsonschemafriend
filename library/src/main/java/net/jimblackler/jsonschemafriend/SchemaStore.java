package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.CacheLoader.load;
import static net.jimblackler.jsonschemafriend.DocumentUtils.parseJson;
import static net.jimblackler.jsonschemafriend.MetaSchemaDetector.detectMetaSchema;
import static net.jimblackler.jsonschemafriend.PathUtils.append;
import static net.jimblackler.jsonschemafriend.PathUtils.baseDocumentFromUri;
import static net.jimblackler.jsonschemafriend.PathUtils.fixUnescaped;
import static net.jimblackler.jsonschemafriend.PathUtils.normalize;
import static net.jimblackler.jsonschemafriend.PathUtils.resolve;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SchemaStore {
  private static final Logger LOG = Logger.getLogger(SchemaStore.class.getName());

  private final Map<URI, Object> canonicalUriToObject = new HashMap<>();
  private final Map<URI, Object> canonicalUriToBaseObject = new HashMap<>();
  private final Map<URI, URI> validUriToCanonicalUri = new HashMap<>();
  private final Map<URI, URI> canonicalUriToResourceUri = new HashMap<>();
  private final Map<URI, Schema> builtSchemas = new HashMap<>();
  private final Collection<URI> mapped = new HashSet<>();
  private final UrlRewriter urlRewriter;
  private int memorySchemaNumber;

  public SchemaStore() {
    urlRewriter = null;
  }

  public SchemaStore(UrlRewriter urlRewriter) {
    this.urlRewriter = urlRewriter;
  }

  public Schema loadSchema(Object document) throws GenerationException {
    // Every document needs a unique, default canonical URI.
    URI uri = URI.create(memorySchemaNumber == 0 ? "" : String.valueOf(memorySchemaNumber));
    memorySchemaNumber++;
    URI canonicalUri = store(uri, document);
    return loadSchema(canonicalUri);
  }

  public Schema loadSchema(File file) throws GenerationException {
    if (!file.isFile()) {
      throw new GenerationException(file + " is not a file.");
    }
    return loadSchema(file.toURI());
  }

  public Schema loadSchema(URL url) throws GenerationException {
    try {
      return loadSchema(url.toURI());
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public Schema loadSchema(URI uri) throws GenerationException {
    return loadSchema(uri, true);
  }

  public Schema loadSchema(URI uri, boolean prevalidate) throws GenerationException {
    uri = normalize(uri);
    while (true) {
      if (builtSchemas.containsKey(uri)) {
        return builtSchemas.get(uri);
      }

      if (validUriToCanonicalUri.containsKey(uri)) {
        uri = validUriToCanonicalUri.get(uri);
        continue;
      }
      if (!canonicalUriToObject.containsKey(uri) && uri.isAbsolute()) {
        // We don't know this canonical URL, so we treat it as a resource URL and try to fetch
        // it.
        URI documentUri = baseDocumentFromUri(uri);
        LOG.fine("Loading: " + documentUri + " to resolve: " + uri);

        try {
          String content;
          if (urlRewriter == null) {
            content = load(documentUri);
          } else {
            content = load(urlRewriter.rewrite(documentUri));
          }
          if (!mapped.contains(documentUri)) {
            store(documentUri, parseJson(content));
            continue;
          }
        } catch (JSONException | IOException e) {
          LOG.warning("Failed attempt to auto fetch to resolve: " + uri);
        }
      }

      Object schemaObject = getObject(uri);
      if (!(schemaObject instanceof JSONObject)) {
        break;
      }
      JSONObject schemaJsonObject = (JSONObject) schemaObject;
      Object refObject = schemaJsonObject.opt("$ref");
      if (refObject instanceof String) {
        String refString = fixUnescaped((String) refObject);
        URI pointingTo = resolve(uri, URI.create(refString));
        URI metaSchema = detectMetaSchema(canonicalUriToBaseObject.get(uri));
        if (metaSchema.equals(MetaSchemaUris.DRAFT_2019_09)) {
          if (schemaJsonObject.length() > 1) {
            break;
          }
        }
        uri = pointingTo;
      } else {
        break;
      }
    }
    // If we can, we fetch and build the schema's meta-schema and validate the object against it,
    // before we attempt to build the Schema.
    // This can't be done inside the Schema builder because the schema's meta-schema might be in its
    // own graph, so the meta-schema won't be built in full when it's first available.

    if (prevalidate) {
      Object schemaObject = getObject(uri);
      if (schemaObject != null) {
        URI metaSchemaUri = detectMetaSchema(canonicalUriToBaseObject.get(uri));
        if (!normalize(metaSchemaUri).equals(uri)) {
          Schema metaSchema = loadSchema(metaSchemaUri, false);
          JSONObject validation =
              new Validator().validateWithOutput(this, metaSchema, schemaObject);
          if (!validation.getBoolean("valid")) {
            throw new StandardGenerationException(validation);
          }
        }
      }
    }

    return new Schema(this, uri);
  }

  public void register(URI path, Schema schema) throws GenerationException {
    if (builtSchemas.put(path, schema) != null) {
      throw new GenerationException(path + " already registered");
    }
  }

  public URI store(URI uri, Object document) {
    if (!mapped.add(uri)) {
      throw new IllegalStateException("Double mapped");
    }
    return map(document, document, uri, uri, detectMetaSchema(document), true);
  }

  URI map(Object object, Object baseObject, URI validUri, URI canonicalBaseUri, URI metaSchema,
      boolean isResource) {
    String idKey =
        (MetaSchemaUris.DRAFT_3.equals(metaSchema) || MetaSchemaUris.DRAFT_4.equals(metaSchema))
        ? "id"
        : "$id";
    URI canonicalUri = canonicalBaseUri;
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Object idObject = jsonObject.opt(idKey);
      if (idObject instanceof String) {
        URI child = URI.create((String) idObject);
        if (MetaSchemaUris.DRAFT_2019_09.equals(metaSchema) && child.getRawFragment() != null
            && !child.getRawFragment().isEmpty()) {
          LOG.warning("Illegal fragment in ID");
        } else {
          canonicalUri = resolve(canonicalUri, child);
        }
      }
      Object anchorObject = jsonObject.opt("$anchor");
      if (anchorObject instanceof String) {
        try {
          URI anchor = new URI(null, null, null, (String) anchorObject);
          canonicalUri = resolve(canonicalUri, anchor);
        } catch (URISyntaxException e) {
          LOG.warning("Problem with $anchor: " + e.getMessage());
        }
      }
    }

    if (isResource) {
      URI was = canonicalUriToResourceUri.put(canonicalUri, validUri);
      if (was != null) {
        LOG.warning("Attempt to map from at least two locations: " + canonicalUri
            + System.lineSeparator() + validUri + System.lineSeparator() + was);
        return canonicalUri;
      }
      if (canonicalUriToObject.put(canonicalUri, object) != null) {
        throw new IllegalStateException(
            "Different content with same IDs found mapping " + canonicalUri);
      }

      if (canonicalUriToBaseObject.put(canonicalUri, baseObject) != null) {
        throw new IllegalStateException(
            "Different content with same IDs found mapping " + canonicalUri);
      }
    }
    if (!canonicalBaseUri.equals(canonicalUri)) {
      validUriToCanonicalUri.put(canonicalBaseUri, canonicalUri);
    }
    if (!validUri.equals(canonicalUri)) {
      validUriToCanonicalUri.put(validUri, canonicalUri);
    }

    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      for (String key : jsonObject.keySet()) {
        map(jsonObject.get(key), baseObject, append(validUri, key), append(canonicalUri, key),
            metaSchema, isResource);
        if (!canonicalBaseUri.equals(canonicalUri) && !canonicalBaseUri.equals(validUri)) {
          map(jsonObject.get(key), baseObject, append(canonicalBaseUri, key),
              append(canonicalUri, key), metaSchema, false);
        }
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        map(jsonArray.get(idx), baseObject, append(validUri, String.valueOf(idx)),
            append(canonicalUri, String.valueOf(idx)), metaSchema, isResource);
      }
    }

    return canonicalUri;
  }

  Object getObject(URI canonicalUri) {
    if (validUriToCanonicalUri.containsKey(canonicalUri)) {
      throw new IllegalStateException("getObject(): non-canonical URL received");
    }
    if (canonicalUriToObject.containsKey(canonicalUri)) {
      return canonicalUriToObject.get(canonicalUri);
    }
    return null;
  }

  public Object getBaseObject(URI uri) {
    return canonicalUriToBaseObject.get(uri);
  }

  public URI canonicalUriToResourceUri(URI uri) {
    return canonicalUriToResourceUri.get(uri);
  }
}
