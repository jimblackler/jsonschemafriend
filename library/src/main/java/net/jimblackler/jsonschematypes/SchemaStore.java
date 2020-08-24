package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONObject;

public class SchemaStore {
  private static final URI ROOT = URI.create("");

  private final Map<URI, Schema> builtSchemas = new HashMap<>();
  private final IdRefMap idRefMap = new IdRefMap();
  private final DocumentSource documentSource;

  public SchemaStore(DocumentSource documentSource) {
    this.documentSource = documentSource;
  }

  Schema validateAndGet(URI uri) throws GenerationException {
    Object document = documentSource.fetchDocument(PathUtils.baseDocumentFromUri(uri));
    Object schemaObject = PathUtils.fetchFromPath(document, uri.getRawFragment());
    // If we can, we fetch and build the schema's meta-schema and validate the object against it,
    // before we attempt to build the Schema.
    // This can't be done inside the Schema builder because the schema's meta-schema might be in its
    // own graph, so the meta-schema won't be built in full when it's first available.
    if (false) {
      if (schemaObject instanceof JSONObject) {
        JSONObject schemaJson = (JSONObject) schemaObject;
        String metaSchemaName = schemaJson.getString("$schema");
        if (!metaSchemaName.isEmpty()) {
          Schema metaSchema = getSchema(URI.create(metaSchemaName));
          List<ValidationError> errors = new ArrayList<>();
          metaSchema.validate(schemaObject, ROOT, errors::add);
          if (!errors.isEmpty()) {
            throw new GenerationException(errors.toString());
          }
        }
      }
    }

    return getSchema(uri);
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }
        validateAndGet(new URI("file", path.toString(), null));
      }
    } catch (IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
    }
  }

  // TODO: Get rid of this somehow.
  Object getBaseDocument(URI path) throws GenerationException {
    return documentSource.fetchDocument(PathUtils.baseDocumentFromUri(path));
  }

  public Schema getSchema(URI uri) throws GenerationException {
    // The schema at an uri can be requested by a client that doesn't know that the uri is the
    // beginning of a chain of $refs. We must find the end of the chain, where the actual schema is
    // to be found.
    uri = idRefMap.finalLocation(uri, documentSource);

    // Now we have the final path we can see if we've already built it.
    if (builtSchemas.containsKey(uri)) {
      return builtSchemas.get(uri);
    }

    Object baseDocument = documentSource.fetchDocument(PathUtils.baseDocumentFromUri(uri));
    Object object = PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new BooleanSchema(this, uri, (boolean) object);
    }

    return new ObjectSchema(this, uri);
  }

  public void register(URI path, Schema schema) throws GenerationException {
    if (builtSchemas.put(path, schema) != null) {
      throw new GenerationException(path + " already registered");
    }
  }

  public DocumentSource getDocumentSource() {
    // TOOO: consider how we can avoid vending a dependency directly like this.
    return documentSource;
  }
}
