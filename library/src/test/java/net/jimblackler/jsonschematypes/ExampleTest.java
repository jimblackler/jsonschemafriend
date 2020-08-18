package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class ExampleTest {
  public static void main(String[] args) {
    Path base = Path.of("/examples");
    scan(base.resolve("fstab"));
    scan(base.resolve("standard/7-7-1-1"));
    scan(base.resolve("misc"));
    scan(base.resolve("longread"));
    scan(base.resolve("meta"));
  }

  private static void scan(Path testDir) {
    System.out.println();
    System.out.println(testDir);
    System.out.println();
    Path fullDir = testDir.resolve("schemas");
    Validator validator = new Validator("classpath://" + fullDir + "/");

    try (InputStream inputStream = ExampleTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;
      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          Object data = Utils.getJsonObject(testDir.resolve(resource).toString());
          validator.validate("schema.json", data);
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }

    try {
      URL resource = ExampleTest.class.getResource(fullDir.toString());
      Main.generateTypes(Path.of("out"), Path.of(resource.toURI()), "org.example");
    } catch (GenerationException | URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
