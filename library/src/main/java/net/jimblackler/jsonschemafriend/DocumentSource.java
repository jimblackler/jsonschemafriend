package net.jimblackler.jsonschemafriend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocumentSource {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();
  private final Iterable<UrlRewriter> rewriters;
  private final Map<URI, Object> memoryCache = new HashMap<>();

  public DocumentSource() {
    rewriters = new ArrayList<>();
  }

  public DocumentSource(Iterable<UrlRewriter> rewriters) {
    this.rewriters = rewriters;
  }

  public Object fetchDocument(URI originalUrl) throws MissingPathException {
    if (memoryCache.containsKey(originalUrl)) {
      return memoryCache.get(originalUrl);
    }

    if (!originalUrl.isAbsolute()) {
      throw new MissingPathException("Not an absolute URL");
    }

    if (originalUrl.getRawFragment() != null && !originalUrl.getRawFragment().isEmpty()) {
      throw new MissingPathException("Not a base document");
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

    Path diskCacheName = FILE_SYSTEM.getPath(System.getProperty("java.io.tmpdir"))
                             .resolve("net.jimblackler.jsonschemafriend")
                             .resolve("cache" + path);
    if (useDiskCache && Files.exists(diskCacheName)) {
      url = diskCacheName.toUri();
      useDiskCache = false;
    }

    String content;

    try {
      content = DocumentUtils.streamToString(url.toURL().openStream());
    } catch (IllegalArgumentException | IOException e) {
      throw new MissingPathException("Error fetching " + url, e);
    }

    Object object = DocumentUtils.parseJson(content);
    memoryCache.put(originalUrl, object);

    if (useDiskCache) {
      diskCacheName.getParent().toFile().mkdirs();
      try (PrintWriter out = new PrintWriter(diskCacheName.toFile())) {
        out.println(content);
      } catch (IOException e) {
        throw new MissingPathException(e);
      }
    }

    return object;
  }

  public void store(URI path, Object document) {
    memoryCache.put(path, document);
  }
}