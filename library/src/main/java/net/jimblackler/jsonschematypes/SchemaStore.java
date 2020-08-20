package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

public class SchemaStore {
  private final Map<URI, Schema> builtPaths = new HashMap<>();
  private final Map<URI, Object> documentCache = new HashMap<>();
  private final Map<URI, URI> idToPath = new HashMap<>();
  private final Map<URI, URI> refs = new HashMap<>();
  private final Collection<UrlRewriter> rewriters = new ArrayList<>();
  private final URI basePointer = URI.create("");

  public Collection<ValidationError> validate(URI uri, Object jsonObject) {
    Schema schema = builtPaths.get(finalPath(uri));
    List<ValidationError> errors = new ArrayList<>();
    schema.validate(jsonObject, errors::add);
    return errors;
  }

  public void addRewriter(UrlRewriter rewriter) {
    rewriters.add(rewriter);
  }

  public void loadBaseObject(Object jsonObject) throws GenerationException {
    documentCache.put(basePointer, jsonObject);
    findIds(basePointer, null);
    getSchema(basePointer);
  }

  private Object fetchDocument(URI url) throws GenerationException {
    for (UrlRewriter rewriter : rewriters) {
      url = rewriter.rewrite(url);
    }
    if (documentCache.containsKey(url)) {
      return documentCache.get(url);
    }
    String content;

    try (Scanner scanner = new Scanner(url.toURL().openStream(), StandardCharsets.UTF_8)) {
      content = scanner.useDelimiter("\\A").next();
    } catch (IllegalArgumentException | IOException e) {
      throw new GenerationException("Error fetching " + url, e);
    }
    Object object;
    try {
      object = new JSONArray(content);
    } catch (JSONException e) {
      object = new JSONObject(content);
    }
    documentCache.put(url, object);
    findIds(url, null);
    return object;
  }

  private void findIds(URI path, URI activeId) throws GenerationException {
    Object object = getSchemaJson(path);
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

      Iterator<String> it = jsonObject.keys();
      while (it.hasNext()) {
        String key = it.next();
        findIds(JsonSchemaRef.append(path, key), activeId);
      }
    } else if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        findIds(JsonSchemaRef.append(path, String.valueOf(idx)), activeId);
      }
    }
  }

  public Object getSchemaJson(URI path) throws GenerationException {
    try {
      URI documentUri = new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
      Object document = fetchDocument(documentUri);
      if (path.getFragment() == null) {
        return document;
      }
      return new JSONPointer("#" + path.getRawFragment()).queryFrom(document);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  /**
   * Converts a URI containing an id or path to the final path of the schema.
   * @param uri The id or path.
   * @return The final path of the schema.
   */
  public URI finalPath(URI uri) {
    if (idToPath.containsKey(uri)) {
      return finalPath(idToPath.get(uri));
    }
    if (refs.containsKey(uri)) {
      return finalPath(refs.get(uri));
    }
    return uri;
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
    path = finalPath(path);
    if (builtPaths.containsKey(path)) {
      return builtPaths.get(path);
    }
    Object object = getSchemaJson(path);

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new BooleanSchema(this, path, (boolean) object);
    }

    return new ObjectSchema(this, path);
  }

  public void register(URI path, Schema schema) throws GenerationException {
    if (builtPaths.containsKey(path)) {
      throw new GenerationException(path + " already registered");
    }
    builtPaths.put(path, schema);
  }

  interface UrlRewriter {
    URI rewrite(URI in);
  }
}
