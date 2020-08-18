package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

public class SchemaStore {
  private final Collection<URI> unbuilt = new HashSet<>();
  private final Map<URI, Schema> built = new HashMap<>();
  private final Map<URI, Object> cache = new HashMap<>();
  private final URI basePointer;

  public SchemaStore() throws GenerationException {
    try {
      basePointer = new URI(null, null, null);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public void loadBaseObject(Object jsonObject) throws GenerationException {
    cache.put(basePointer, jsonObject);
    require(basePointer);
  }

  Object load(URI uri) throws GenerationException {
    if (cache.containsKey(uri)) {
      return cache.get(uri);
    }
    String content;

    try (Scanner scanner = new Scanner(uri.toURL().openStream(), StandardCharsets.UTF_8)) {
      content = scanner.useDelimiter("\\A").next();
    } catch (IOException e) {
      throw new GenerationException(e);
    }
    Object jsonObject;
    try {
      jsonObject = new JSONArray(content);
    } catch (JSONException e) {
      try {
        jsonObject = new JSONObject(content);
      } catch (JSONException e2) {
        throw new GenerationException(e2);
      }
    }
    cache.put(uri, jsonObject);
    return jsonObject;
  }

  public Object resolve(URI uri) throws GenerationException {
    try {
      URI minusFragment = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), null);
      Object object = load(minusFragment);
      if (uri.getFragment() == null || "/".equals(uri.getFragment())) {
        return object;
      }
      JSONPointer jsonPointer = new JSONPointer("#" + uri.getFragment());
      return jsonPointer.queryFrom(object);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public URI require(URI uri) throws GenerationException {
    if (unbuilt.contains(uri) || built.containsKey(uri)) {
      return uri;
    }
    // Is a $ref?
    Object object = resolve(uri);
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      String ref = jsonObject.optString("$ref");
      if (!ref.isEmpty()) {
        // TODO: first walk up the tree looking for the first $id ancestor, for the base
        // rather than that of the active JSON Pointer.
        // See "This URI also serves as the base URI.." in
        // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-8.2.2
        URI uri1 = uri.resolve(ref);
        return require(uri1);
      }
    }
    unbuilt.add(uri);
    return uri;
  }

  public void process() throws GenerationException {
    while (!unbuilt.isEmpty()) {
      URI uri = unbuilt.iterator().next();
      System.out.println("Processing " + uri);
      built.put(uri, Schemas.create(this, uri));
      unbuilt.remove(uri);
    }
  }

  public void loadResources(Path resources) throws GenerationException {
    try (Stream<Path> walk = Files.walk(resources)) {
      for (Path path : walk.collect(Collectors.toList())) {
        if (Files.isDirectory(path)) {
          continue;
        }

        require(new URI("file", path.toString(), null));
      }
    } catch (UncheckedGenerationException | IOException | URISyntaxException ex) {
      throw new GenerationException(ex);
    }
  }
}
