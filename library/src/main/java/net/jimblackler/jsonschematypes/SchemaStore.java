package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaStore {
  private static final URI ROOT = URI.create("");

  private final Map<URI, Schema> builtPaths = new HashMap<>();
  private final IdRefMap idRefMap = new IdRefMap();
  private final Collection<URI> mapped = new HashSet<>();
  private final DocumentSource documentSource;

  public SchemaStore(DocumentSource documentSource) {
    this.documentSource = documentSource;
  }

  public Schema getSchemaFromJson(Object object, URI path) throws GenerationException {
    documentSource.store(path, object);
    if (mapped.add(path)) {
      idRefMap.map(this, path);
    }
    return getAndValidate(path);
  }

  private Schema getAndValidate(URI document) throws GenerationException {
    Schema schema = getSchema(document);
    // We validate the schema JSON against its metaschema AFTER the Schema object has been built
    // from that JSON. It has to be done in that order because the schema and the metaschema could
    // be the same thing.
    Schema metaSchema = schema.getMetaSchema();
    List<ValidationError> errors = new ArrayList<>();
    if (metaSchema != null) {
      metaSchema.validate(getSchemaJson(document), ROOT, errors::add);
      if (!errors.isEmpty()) {
        throw new GenerationException(errors.toString());
      }
    }
    return schema;
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }
        getAndValidate(new URI("file", path.toString(), null));
      }
    } catch (IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
    }
  }

  public Object getSchemaJson(URI path) throws GenerationException {
    try {
      URI documentUri = new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
      Object document = documentSource.fetchDocument(documentUri);
      if (mapped.add(documentUri)) {
        idRefMap.map(this, documentUri);
      }
      return PathUtils.objectAtPath(document, path);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public Schema getSchema(URI path) throws GenerationException {
    // The schema at a path can be requested by a client that doesn't know that the path is the
    // beginning of a chain of $refs. We must find the end of the chain, where the actual schema is
    // to be found.
    path = idRefMap.finalPath(path);

    // Now we have the final path we can see if we've already built it.
    if (builtPaths.containsKey(path)) {
      return builtPaths.get(path);
    }

    // We fetch the actual JSON definition.
    Object object = getSchemaJson(path);

    // In the case the path is to a $ref object in a document that hadn't yet been mapped, the final
    // path will have changed underneath us, and we'll have to start again.
    URI finalPath = idRefMap.finalPath(path);
    if (!finalPath.equals(path)) {
      return getSchema(finalPath);
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new BooleanSchema(this, path, (boolean) object);
    }

    return new ObjectSchema(this, path);
  }

  public void register(URI path, Schema schema) throws GenerationException {
    if (builtPaths.put(path, schema) != null) {
      throw new GenerationException(path + " already registered");
    }
  }
}
