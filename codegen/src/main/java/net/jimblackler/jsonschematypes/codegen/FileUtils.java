package net.jimblackler.jsonschematypes.codegen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileUtils {
  public static void createOrEmpty(Path outPath) throws IOException {
    if (Files.exists(outPath)) {
      // Empty the directory if it already exists.
      try (Stream<Path> files = Files.walk(outPath)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
    Files.createDirectories(outPath);
  }
}
