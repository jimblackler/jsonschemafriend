package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class CacheLoader {
  private static final Logger LOG = Logger.getLogger(CacheLoader.class.getName());
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static String load(URI uri) throws IOException {
    if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
      Path diskCacheName = FILE_SYSTEM.getPath(System.getProperty("java.io.tmpdir"))
                               .resolve("net.jimblackler.jsonschemafriend")
                               .resolve("cache")
                               .resolve(uri.getHost() + uri.getPath());
      if (Files.exists(diskCacheName)) {
        LOG.fine("Cache loading: " + uri + System.lineSeparator() + "From: " + diskCacheName);
        return streamToString(diskCacheName.toUri().toURL().openStream());
      }
      String content = streamToString(uri.toURL().openStream());
      diskCacheName.getParent().toFile().mkdirs();
      try (PrintWriter out = new PrintWriter(diskCacheName.toFile())) {
        out.println(content);
      }
      return content;
    }
    LOG.fine("Loading :" + uri);
    return streamToString(uri.toURL().openStream());
  }
}
