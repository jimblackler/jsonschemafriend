package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentSource {
  private final Iterable<UrlRewriter> rewriters;
  private final Map<URI, Object> cache = new HashMap<>();

  DocumentSource(Iterable<UrlRewriter> rewriters) {
    this.rewriters = rewriters;
  }

  public Object fetchDocument(URI url) throws GenerationException {
    for (UrlRewriter rewriter : rewriters) {
      url = rewriter.rewrite(url);
    }
    if (cache.containsKey(url)) {
      return cache.get(url);
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
    cache.put(url, object);
    return object;
  }

  public void store(URI basePointer, Object jsonObject) {
    cache.put(basePointer, jsonObject);
  }
}