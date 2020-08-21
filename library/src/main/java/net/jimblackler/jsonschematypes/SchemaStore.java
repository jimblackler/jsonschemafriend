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
  private final Map<URI, Schema> builtPaths = new HashMap<>();
  private final URI basePointer = URI.create("");
  private final IdRefMap idRefMap = new IdRefMap();
  private final Collection<URI> mapped = new HashSet<>();
  private final DocumentSource documentSource;

  public SchemaStore(DocumentSource documentSource) {
    this.documentSource = documentSource;
  }

  public Collection<ValidationError> validate(URI uri, Object jsonObject) {
    URI finalPath = idRefMap.finalPath(uri);
    Schema schema = builtPaths.get(finalPath);
    List<ValidationError> errors = new ArrayList<>();
    schema.validate(jsonObject, URI.create(""), errors::add);
    return errors;
  }

  public void loadBaseObject(Object jsonObject) throws GenerationException {
    documentSource.store(basePointer, jsonObject);
    if (mapped.add(basePointer)) {
      idRefMap.map(this, basePointer, basePointer);
    }
    // Parsing the schema JSON can cause the final path to change (e.g. in the case of a $ref on
    // the root element
    URI path = idRefMap.finalPath(basePointer);
    getSchema(path);
  }

  public Object getSchemaJson(URI path) throws GenerationException {
    try {
      URI documentUri = new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
      Object document = documentSource.fetchDocument(documentUri);
      if (mapped.add(documentUri)) {
        idRefMap.map(this, documentUri, documentUri);
      }
      return PathUtils.objectAtPath(document, path);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }
        getSchema(new URI("file", path.toString(), null));
      }
    } catch (IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
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
