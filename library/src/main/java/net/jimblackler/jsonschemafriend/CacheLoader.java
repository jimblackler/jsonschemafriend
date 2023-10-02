package net.jimblackler.jsonschemafriend;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class CacheLoader implements Loader {
  private static final Logger LOG = Logger.getLogger(CacheLoader.class.getName());
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public String load(URI uri, boolean cacheSchema) throws IOException {
    if (cacheSchema && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
      Path diskCacheName = FILE_SYSTEM.getPath(System.getProperty("java.io.tmpdir"))
                               .resolve("net.jimblackler.jsonschemafriend")
                               .resolve("cache")
                               .resolve(uri.getHost() + uri.getPath());
      if (Files.exists(diskCacheName)) {
        LOG.fine("Cache loading: " + uri + System.lineSeparator() + "From: " + diskCacheName);
        return UrlUtils.readFromStream(diskCacheName.toUri().toURL());
      }
      String content = UrlUtils.readFromStream(uri.toURL());
      diskCacheName.getParent().toFile().mkdirs();
      try (PrintWriter out = new PrintWriter(diskCacheName.toFile())) {
        out.println(content);
      }
      return content;
    }
    LOG.fine("Loading :" + uri);

    return UrlUtils.readFromStream(uri.toURL());
  }
}
