package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ResourcesSchemaStore extends SchemaStore {
  private final Map<URI, Object> cache = new HashMap<>();
  private final Path resources;

  public ResourcesSchemaStore(Path resources) throws GenerationException {
    this.resources = resources;
    try {
      try (Stream<Path> walk = Files.walk(resources)) {
        for (Path path : walk.collect(Collectors.toList())) {
          if (Files.isDirectory(path)) {
            continue;
          }
          Path relativePath = resources.relativize(path);
          URI uri = new URI(null, null, null, -1, relativePath.toString(), null, "/");
          require(uri);
        }
      } catch (URISyntaxException e) {
        throw new GenerationException(e);
      }
    } catch (UncheckedGenerationException | IOException ex) {
      throw new GenerationException(ex);
    }
  }

  public Object load(URI path) throws GenerationException {
    if (!"classpath".equals(path.getScheme())) {
      throw new GenerationException("Classpath expected");
    }
    if (cache.containsKey(path)) {
      return cache.get(path);
    }
    String content;
    try {
      content = Files.readString(resources.resolve(path.getPath()));
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
    cache.put(path, jsonObject);
    return jsonObject;
  }
}
