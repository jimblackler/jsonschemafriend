package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DocumentSource {
  private final Iterable<UrlRewriter> rewriters;
  private final Map<URI, Object> memoryCache = new HashMap<>();

  public DocumentSource(Iterable<UrlRewriter> rewriters) {
    this.rewriters = rewriters;
  }

  public Object fetchDocument(URI url) throws GenerationException {
    for (UrlRewriter rewriter : rewriters) {
      url = rewriter.rewrite(url);
    }
    if (memoryCache.containsKey(url)) {
      return memoryCache.get(url);
    }

    boolean useDiskCache = "http".equals(url.getScheme()) || "https".equals(url.getScheme());

    Path diskCacheName = Path.of("cache").resolve(url + ".json");
    if (useDiskCache && Files.exists(diskCacheName)) {
      url = diskCacheName.toUri();
      useDiskCache = false;
    }

    String content;

    try (Scanner scanner = new Scanner(url.toURL().openStream(), StandardCharsets.UTF_8)) {
      content = scanner.useDelimiter("\\A").next();
    } catch (IllegalArgumentException | IOException e) {
      throw new GenerationException("Error fetching " + url, e);
    }

    if (useDiskCache) {
      diskCacheName.getParent().toFile().mkdirs();
      try (PrintWriter out = new PrintWriter(diskCacheName.toFile())) {
        out.println(content);
      } catch (IOException e) {
        throw new GenerationException(e);
      }
    }

    Object object;
    try {
      object = new JSONArray(content);
    } catch (JSONException e) {
      object = new JSONObject(content);
    }
    memoryCache.put(url, object);
    return object;
  }

  public void store(URI basePointer, Object jsonObject) {
    memoryCache.put(basePointer, jsonObject);
  }
}