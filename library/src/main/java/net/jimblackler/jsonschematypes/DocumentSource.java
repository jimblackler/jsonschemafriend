package net.jimblackler.jsonschematypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class DocumentSource {
  private final Iterable<UrlRewriter> rewriters;
  private final Map<URI, Object> memoryCache = new HashMap<>();

  public DocumentSource(Iterable<UrlRewriter> rewriters) {
    this.rewriters = rewriters;
  }

  private static String streamToString(InputStream inputStream) {
    String content;
    try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
      content = scanner.useDelimiter("\\A").next();
    }
    return content;
  }

  public Object fetchDocument(URI originalUrl) throws GenerationException {
    if (memoryCache.containsKey(originalUrl)) {
      return memoryCache.get(originalUrl);
    }
    if (originalUrl.getRawFragment() != null && !originalUrl.getRawFragment().isEmpty()) {
      throw new GenerationException("Not a base document");
    }

    URI url = originalUrl;
    for (UrlRewriter rewriter : rewriters) {
      url = rewriter.rewrite(url);
    }

    boolean useDiskCache = "http".equals(url.getScheme()) || "https".equals(url.getScheme());

    String path = url.getSchemeSpecificPart();
    if (!path.endsWith(".json")) {
      path += ".json";
    }
    Path diskCacheName = Path.of("cache" + path);
    if (useDiskCache && Files.exists(diskCacheName)) {
      url = diskCacheName.toUri();
      useDiskCache = false;
    }

    String content;

    try {
      content = streamToString(url.toURL().openStream());
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

    content = content.replaceAll("[\uFEFF-\uFFFF]", ""); // Strip the dreaded FEFF.
    content = content.strip();
    Object object;
    char firstChar = content.charAt(0);
    if (firstChar == '[') {
      object = new JSONArray(content);
    } else if (firstChar == '{') {
      object = new JSONObject(content);
    } else {
      throw new GenerationException(originalUrl + " doesn't look like JSON.");
    }
    memoryCache.put(originalUrl, object);
    return object;
  }

  public void store(URI path, Object document) {
    memoryCache.put(path, document);
  }
}