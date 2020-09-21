package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class CacheLoader {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static String load(URI uri) throws IOException {
    Path diskCacheName = FILE_SYSTEM.getPath(System.getProperty("java.io.tmpdir"))
                             .resolve("net.jimblackler.jsonschemafriend")
                             .resolve("cache")
                             .resolve(uri.getHost() + uri.getPath());
    if (Files.exists(diskCacheName)) {
      return streamToString(diskCacheName.toUri().toURL().openStream());
    }
    String content = streamToString(uri.toURL().openStream());
    diskCacheName.getParent().toFile().mkdirs();
    try (PrintWriter out = new PrintWriter(diskCacheName.toFile())) {
      out.println(content);
    }
    return content;
  }
}
