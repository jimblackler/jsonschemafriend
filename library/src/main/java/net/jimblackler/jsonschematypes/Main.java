package net.jimblackler.jsonschematypes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

      List<Schema> schemas = new ArrayList<>();
      try (Stream<Path> walk = Files.walk(resources)) {
        for (Path path : walk.collect(Collectors.toList())) {
          if (Files.isDirectory(path)) {
            continue;
          }

          String content = Files.readString(path);
          Object jsonObject;
          try {
            jsonObject = new JSONArray(content);
          } catch (JSONException e) {
            try {
              jsonObject = new JSONObject(content);
            } catch (JSONException e2) {
              throw new GenerationException(e2);
            }
          }

          schemas.add(Schemas.create(
              new BaseSchemaContext(resources.relativize(path).toString()), jsonObject));
        }
      }
    } catch (UncheckedGenerationException | IOException ex) {
      throw new GenerationException(ex);
    }
  }
}
