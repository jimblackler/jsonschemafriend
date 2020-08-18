package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SingleObjectSchemaStore extends SchemaStore {
  private final Map<URI, Object> cache = new HashMap<>();
  private final URI basePointer;

  public SingleObjectSchemaStore(Object jsonObject) throws GenerationException {

    try {
      basePointer = new URI(null, null, null, -1, null, null, null);
      cache.put(basePointer, jsonObject);
      require(basePointer);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  @Override
  Object load(URI uri) throws GenerationException {
    if (cache.containsKey(uri)) {
      return cache.get(uri);
    }
    try {
      try (Scanner scanner = new Scanner(uri.toURL().openStream(), StandardCharsets.UTF_8)) {
        String content = scanner.useDelimiter("\\A").next();
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
    } catch (IOException e) {
      throw new GenerationException(e);
    }
  }
}
