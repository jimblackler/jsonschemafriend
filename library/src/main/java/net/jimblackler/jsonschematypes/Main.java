package net.jimblackler.jsonschematypes;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
  public static void generateTypes(Path outPath, Path resources, String _package)
      throws GenerationException {
    try {
      if (Files.exists(outPath)) {
        // Empty the directory if it already exists.
        try (Stream<Path> files = Files.walk(outPath)) {
          files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
      }
      Files.createDirectories(outPath);

      SchemaStore schemaStore = new SchemaStore(resources);
      try (Stream<Path> walk = Files.walk(resources)) {
        for (Path path : walk.collect(Collectors.toList())) {
          if (Files.isDirectory(path)) {
            continue;
          }
          schemaStore.require(
              new URI(null, null, null, -1, resources.relativize(path).toString(), null, "/"));
        }
      } catch (URISyntaxException e) {
        throw new GenerationException(e);
      }
      schemaStore.process();

    } catch (UncheckedGenerationException | IOException ex) {
      throw new GenerationException(ex);
    }
  }
}
