package net.jimblackler.codegen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Schema;
import net.jimblackler.jsonschematypes.SchemaStore;

public class Main2 {
  public static void outputTypes(Path outPath, String packageName, URL resource1)
      throws IOException {
    if (Files.exists(outPath)) {
      // Empty the directory if it already exists.
      try (Stream<Path> files = Files.walk(outPath)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
    Files.createDirectories(outPath);

    SchemaStore schemaStore = new SchemaStore();
    URI defaultMetaSchema = URI.create("http://json-schema.org/draft-07/schema#");

    try (InputStream stream = resource1.openStream()) {
      try (BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (resource.endsWith(".json")) {
            URI uri = URI.create(resource1 + "/" + resource);
            Schema schema = schemaStore.validateAndGet(uri, defaultMetaSchema);
            CodeGenerator.generate(schema, outPath, packageName);
          }
        }
      } catch (IOException | GenerationException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
