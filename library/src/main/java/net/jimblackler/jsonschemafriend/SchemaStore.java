package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.CacheLoader.load;
import static net.jimblackler.jsonschemafriend.MetaSchemaDetector.detectMetaSchema;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_3;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_4;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_6;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_7;
import static net.jimblackler.jsonschemafriend.PathUtils.append;
import static net.jimblackler.jsonschemafriend.PathUtils.baseDocumentFromUri;
import static net.jimblackler.jsonschemafriend.PathUtils.fixUnescaped;
import static net.jimblackler.jsonschemafriend.PathUtils.normalize;
import static net.jimblackler.jsonschemafriend.PathUtils.resolve;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SchemaStore {
  private static final Logger LOG = Logger.getLogger(SchemaStore.class.getName());

  private final Map<URI, Object> canonicalUriToObject = new HashMap<>();
  private final Map<URI, Object> canonicalUriToBaseObject = new HashMap<>();
  private final Map<URI, URI> validUriToCanonicalUri = new HashMap<>();
  private final Map<URI, URI> canonicalUriToResourceUri = new HashMap<>();
  private final Map<URI, Schema> builtSchemas = new HashMap<>();
  private final Map<URI, Set<String>> dynamicAnchorsBySchemaResource = new HashMap<>();
  private final Collection<URI> mapped = new HashSet<>();
  private final UrlRewriter urlRewriter;
  private int memorySchemaNumber;
  private boolean cacheSchema;

  public SchemaStore() {
    urlRewriter = null;
  }

  public SchemaStore(boolean cacheSchema) {
    this.cacheSchema = cacheSchema;
    urlRewriter = null;
  }

  public SchemaStore(UrlRewriter urlRewriter) {
    this.urlRewriter = urlRewriter;
  }

  public SchemaStore(UrlRewriter urlRewriter, boolean cacheSchema) {
    this.urlRewriter = urlRewriter;
    this.cacheSchema = cacheSchema;
  }

  public Schema loadSchema(Object document) throws GenerationException {
    return loadSchema(document, new Validator());
  }

  public Schema loadSchema(Object document, Validator validator) throws GenerationException {
    // Every document needs a unique, default canonical URI.
    URI uri = URI.create(memorySchemaNumber == 0 ? "" : String.valueOf(memorySchemaNumber));
    memorySchemaNumber++;
    URI canonicalUri = store(uri, document);
    return loadSchema(canonicalUri, validator);
  }

  public Schema loadSchema(File file) throws GenerationException {
    if (!file.isFile()) {
      throw new GenerationException(file + " is not a file.");
    }
    return loadSchema(file.toURI());
  }

  public Schema loadSchema(URL url) throws GenerationException {
    return loadSchema(url, new Validator(), null);
  }

  public Schema loadSchema(URL url, Validator validator, Consumer<ValidationError> errorConsumer)
      throws GenerationException {
    try {
      return loadSchema(url.toURI(), validator, errorConsumer);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public Schema loadSchema(URI uri) throws GenerationException {
    return loadSchema(uri, new Validator());
  }

  public Schema loadSchema(InputStream stream) throws GenerationException {
    try {
      return loadSchemaJson(StreamUtils.streamToString(stream));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public Schema loadSchemaJson(String string) throws GenerationException {
    try {
      return loadSchema(new ObjectMapper().readValue(string, Object.class));
    } catch (JsonProcessingException e) {
      throw new GenerationException(e);
    }
  }

  public Schema loadSchema(URI uri, Validator validator) throws GenerationException {
    return loadSchema(uri, validator, null);
  }

  public Schema loadSchema(URI uri, Validator validator, Consumer<ValidationError> errorConsumer)
      throws GenerationException {
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
          if (!mapped.contains(documentUri)) {
            String content = getContent(documentUri);
            try {
              store(documentUri, new ObjectMapper().readValue(content, Object.class));
            } catch (JsonProcessingException e) {
              // This is a special method designed to handle the JavaScript-based redirection (not
              // http) on the web page at http://json-schema.org/schema.
              // If the loaded content is an HTML page with a canonical reference to another
              // destination. we make a one-off attempt to fetch the document at that destination.
              Document doc = Jsoup.parse(content);
              Elements links = doc.head().select("link[rel='canonical']");
              boolean resolved = false;
              for (Element link : links) {
                String href = link.attr("href");
                URI canonical = new URL(documentUri.toURL(), href).toURI();
                if (!canonical.equals(documentUri)) {
                  store(documentUri, getContent(canonical));
                  resolved = true;
                  break;
                }
              }
              if (!resolved) {
                LOG.warning("Was not valid JSON: " + uri);
              }
            }
            continue;
          }
        } catch (IOException | URISyntaxException e) {
          LOG.warning("Failed attempt to auto fetch to resolve: " + uri);
        }
      }

      Object schemaObject = getObject(uri);
      if (!(schemaObject instanceof Map)) {
        break;
      }
      Map<String, Object> schemaJsonObject = (Map<String, Object>) schemaObject;
      Object refObject = schemaJsonObject.get("$ref");
      if (refObject instanceof String) {
        String refString = fixUnescaped((String) refObject);
        URI pointingTo = resolve(uri, URI.create(refString));
        URI metaSchema = detectMetaSchema(canonicalUriToBaseObject.get(uri));
        boolean preDraft4 = metaSchema.equals(DRAFT_3);
        boolean preDraft6 = preDraft4 || metaSchema.equals(DRAFT_4);
        boolean preDraft7 = preDraft6 || metaSchema.equals(DRAFT_6);
        boolean preDraft2019 = preDraft7 || metaSchema.equals(DRAFT_7);
        if (!preDraft2019 && schemaJsonObject.size() > 1) {
          break;
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
    if (validator != null) {
      Object schemaObject = getObject(uri);
      if (schemaObject != null) {
        URI metaSchemaUri = detectMetaSchema(canonicalUriToBaseObject.get(uri));
        if (!normalize(metaSchemaUri).equals(uri)) {
          Schema metaSchema = loadSchema(metaSchemaUri, null);
          if (errorConsumer == null) {
            Map<String, Object> validation = validator.validateWithOutput(metaSchema, schemaObject);
            if (!(boolean) validation.get("valid")) {
              throw new StandardGenerationException(validation);
            }
          } else {
            validator.validate(metaSchema, schemaObject, errorConsumer);
          }
        }
      }
    }

    return new Schema(this, uri);
  }

  private String getContent(URI documentUri) throws IOException {
    if (urlRewriter == null) {
      return load(documentUri, cacheSchema);
    }
    return load(urlRewriter.rewrite(documentUri), cacheSchema);
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
    return map(document, document, uri, uri, detectMetaSchema(document), true, Keywords.SCHEMA);
  }

  URI map(Object object, Object baseObject, URI validUri, URI canonicalBaseUri, URI metaSchema,
      boolean isResource, int context) {
    URI canonicalUri = canonicalBaseUri;
    if ((context & Keywords.SCHEMA) != 0 && object instanceof Map) {
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      boolean preDraft4 = metaSchema.equals(DRAFT_3);
      boolean preDraft6 = preDraft4 || metaSchema.equals(DRAFT_4);
      boolean preDraft7 = preDraft6 || metaSchema.equals(DRAFT_6);
      boolean preDraft2019 = preDraft7 || metaSchema.equals(DRAFT_7);
      String idKey = preDraft6 ? "id" : "$id";
      Object idObject = jsonObject.get(idKey);
      if (idObject instanceof String) {
        URI child = URI.create((String) idObject);
        if (preDraft2019 && jsonObject.containsKey("$ref")) {
          LOG.warning("$id and $ref together are invalid");
        } else if (!preDraft2019 && child.getRawFragment() != null
            && !child.getRawFragment().isEmpty()) {
          LOG.warning("Illegal fragment in ID");
        } else {
          canonicalUri = resolve(canonicalUri, child);
        }
      }
      Object anchorObject = jsonObject.get("$anchor");
      if (anchorObject instanceof String) {
        try {
          URI anchor = new URI(null, null, null, (String) anchorObject);
          canonicalUri = resolve(canonicalUri, anchor);
        } catch (URISyntaxException e) {
          LOG.warning("Problem with $anchor: " + e.getMessage());
        }
      }

      Object dynamicAnchorObject = jsonObject.get("$dynamicAnchor");
      if (dynamicAnchorObject instanceof String) {
        try {
          URI dynamicAnchorUri = new URI(null, null, null, (String) dynamicAnchorObject);
          // This is for the situations where a $dynamicAnchor should behave like a normal $anchor.
          validUriToCanonicalUri.put(resolve(canonicalUri, dynamicAnchorUri), canonicalUri);
          URI schemaResource = new URI(
              canonicalUri.getScheme(), canonicalUri.getHost(), canonicalUri.getPath(), null);
          dynamicAnchorsBySchemaResource.computeIfAbsent(schemaResource, k -> new HashSet<>())
              .add(dynamicAnchorUri.getFragment());
        } catch (URISyntaxException e) {
          LOG.warning("Problem with $dynamicAnchor: " + e.getMessage());
        }
      }
    }

    if (((context & Keywords.SCHEMA) != 0 || (context & Keywords.MAP) != 0) && isResource) {
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
    if ((context & Keywords.SCHEMA) != 0) {
      if (!canonicalBaseUri.equals(canonicalUri)) {
        URI was = validUriToCanonicalUri.put(canonicalBaseUri, canonicalUri);
        if (was != null && !was.equals(canonicalUri)) {
          LOG.warning("Error mapping " + canonicalBaseUri + " to " + canonicalUri
              + System.lineSeparator() + "Location was already mapped to " + was);
        }
      }
      if (!validUri.equals(canonicalUri) && !canonicalBaseUri.equals(validUri)) {
        URI was = validUriToCanonicalUri.put(validUri, canonicalUri);
        if (was != null && !was.equals(canonicalUri)) {
          LOG.warning("Error mapping " + validUri + " to " + canonicalUri + System.lineSeparator()
              + "Location was already mapped to " + was);
        }
      }

      if (object instanceof Map) {
        Map<String, Object> jsonObject = (Map<String, Object>) object;
        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
          String key = entry.getKey();
          Integer mapContext = Keywords.KEY_TYPES.get(key);
          URI nextCanonical = append(canonicalUri, key);
          if (mapContext == null) {
            mapContext = Keywords.MAP; // Fallback for unexpected keyword.
          }
          if (mapContext == 0) {
            continue;
          }
          map(entry.getValue(), baseObject, append(validUri, key), nextCanonical, metaSchema,
              isResource, mapContext);
          if (canonicalBaseUri.equals(canonicalUri) || canonicalBaseUri.equals(validUri)) {
            continue;
          }
          map(entry.getValue(), baseObject, append(canonicalBaseUri, key), nextCanonical,
              metaSchema, false, mapContext);
        }
      }
    }

    if (((context & Keywords.SCHEMA) == 0 || (context & Keywords.MAP_OF_SCHEMAS) != 0)
        && object instanceof Map) {
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
        String key = entry.getKey();
        URI nextCanonical = append(canonicalUri, key);
        if (validUriToCanonicalUri.containsKey(nextCanonical)) {
          continue;
        }
        map(entry.getValue(), baseObject, append(validUri, key), nextCanonical, metaSchema,
            isResource, (context & Keywords.MAP_OF_SCHEMAS) == 0 ? 0 : Keywords.SCHEMA);
      }
    }

    if (((context & Keywords.SCHEMA) == 0 || (context & Keywords.LIST_OF_SCHEMAS) != 0)
        && object instanceof List) {
      List<Object> jsonArray = (List<Object>) object;
      for (int idx = 0; idx != jsonArray.size(); idx++) {
        map(jsonArray.get(idx), baseObject, append(validUri, String.valueOf(idx)),
            append(canonicalUri, String.valueOf(idx)), metaSchema, isResource,
            (context & Keywords.LIST_OF_SCHEMAS) == 0 ? 0 : Keywords.SCHEMA);
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

  public Set<String> getDynamicAnchorsForSchemaResource(URI uri) {
    return dynamicAnchorsBySchemaResource.get(uri);
  }
}
