package net.jimblackler.jsonschematypes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class Main2 {
  public static void outputTypes(Path outPath, SchemaStore schemaStore, String packageName)
      throws GenerationException {
    try {
      if (Files.exists(outPath)) {
        // Empty the directory if it already exists.
        try (Stream<Path> files = Files.walk(outPath)) {
          files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
      }
      Files.createDirectories(outPath);

    } catch (IOException e) {
      throw new GenerationException(e);
    }
  }
}
