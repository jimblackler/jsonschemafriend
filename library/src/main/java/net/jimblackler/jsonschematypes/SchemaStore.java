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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;
import org.json.JSONPointerException;

public class SchemaStore {
  private final Collection<URI> unbuiltPaths = new HashSet<>();
  private final Map<URI, Schema> builtPaths = new HashMap<>();
  private final Map<URI, Object> documentCache = new HashMap<>();
  private final Map<URI, URI> idToPath = new HashMap<>();
  private final Map<URI, URI> refs = new HashMap<>();
  private final Collection<UrlRewriter> rewriters = new ArrayList<>();
  private final URI basePointer;

  public SchemaStore() throws GenerationException {
    try {
      basePointer = new URI(null, null, null);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

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
    storeDocument(basePointer, jsonObject);
    followAndQueue(basePointer);
  }

  Object fetchDocument(URI url) throws GenerationException {
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
      try {
        object = new JSONObject(content);
      } catch (JSONException e2) {
        throw new GenerationException(e2);
      }
    }
    storeDocument(url, object);
    return object;
  }

  private void storeDocument(URI url, Object object) throws GenerationException {
    documentCache.put(url, object);
    findIds(url, null);
  }

  private void findIds(URI path, URI activeId) throws GenerationException {
    Object object = resolvePath(path);
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
        activeId = newId;
        idToPath.put(activeId, path);
      }

      Object refObject = jsonObject.opt("$ref");
      if (refObject instanceof String) {
        String refObject1 = (String) refObject;
        URI refUri = URI.create(refObject1);
        URI uri1;
        if (activeId == null || refObject1.startsWith("#")) {
          uri1 = path.resolve(refUri);
        } else {
          uri1 = activeId.resolve(refUri);
        }
        refs.put(path, uri1);
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

  public Object resolve(URI idOrPath) throws GenerationException {
    if (idToPath.containsKey(idOrPath)) {
      return resolvePath(idToPath.get(idOrPath));
    }
    return resolvePath(idOrPath);
  }
  public Object resolvePath(URI path) throws GenerationException {
    try {
      URI documentUri = new URI(path.getScheme(), path.getSchemeSpecificPart(), null);
      Object object = fetchDocument(documentUri);
      if (path.getFragment() == null || "/".equals(path.getFragment())) {
        return object;
      }
      String pointerText = "#" + path.getRawFragment();
      try {
        JSONPointer jsonPointer = new JSONPointer(pointerText);
        return jsonPointer.queryFrom(object);
      } catch (JSONPointerException | IllegalArgumentException ex) {
        throw new GenerationException("Problem with pointer " + pointerText, ex);
      }

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

  public URI followAndQueue(URI path) {
    if (unbuiltPaths.contains(path) || builtPaths.containsKey(path)) {
      return path;
    }

    if (refs.containsKey(path)) {
      URI uri1 = refs.get(path);
      // This could be an ID. Convert it back to a path.
      if (idToPath.containsKey(uri1)) {
        uri1 = idToPath.get(uri1);
      }
      return followAndQueue(uri1);
    }
    unbuiltPaths.add(path);
    return path;
  }

  public void process() throws GenerationException {
    while (!unbuiltPaths.isEmpty()) {
      URI path = unbuiltPaths.iterator().next();
      System.out.println("Processing " + path);
      Schemas.create(this, path);
    }
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }
        followAndQueue(new URI("file", path.toString(), null));
      }
    } catch (UncheckedGenerationException | IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
    }
  }

  public Schema build(URI path) throws GenerationException {
    // TODO: try and simplify this by doing away with unbuiltPaths.
    path = followAndQueue(path);
    if (builtPaths.containsKey(path)) {
      return builtPaths.get(path);
    }
    System.out.println("On demand processing " + path);
    return Schemas.create(this, path);
  }

  public void register(URI path, Schema schema) throws GenerationException {
    if (!unbuiltPaths.contains(path)) {
      throw new GenerationException(path + " not in unbuilt");
    }
    if (builtPaths.containsKey(path)) {
      throw new GenerationException(path + " already registered");
    }
    unbuiltPaths.remove(path);
    builtPaths.put(path, schema);
  }

  interface UrlRewriter {
    URI rewrite(URI in);
  }
}
