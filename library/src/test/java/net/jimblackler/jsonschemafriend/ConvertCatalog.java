package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.DocumentUtils.loadJson;
import static net.jimblackler.jsonschemafriend.ReaderUtils.getLines;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvertCatalog {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    Map<String, Object> out = new LinkedHashMap<>();
    Map<String, Object> demos = new LinkedHashMap<>();
    out.put("demos", demos);
    Map<String, Object> schemas = new LinkedHashMap<>();
    out.put("schemas", schemas);

    List<Object> allDemos = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");
    getLines(SuiteTest.class.getResourceAsStream(testDir.toString()), resource -> {
      Path testSchema = schemaPath.resolve(resource + ".json");
      InputStream resourceAsStream =
          ConvertCatalog.class.getResourceAsStream(testSchema.toString());
      if (resourceAsStream == null) {
        System.out.println("Couldn't get " + testSchema);
        return;
      }
      try {
        schemas.put(resource, loadJson(resourceAsStream));
      } catch (IOException e) {
        e.printStackTrace();
        return;
      }

      Path directoryPath = testDir.resolve(resource);
      getLines(ConvertCatalog.class.getResourceAsStream(directoryPath.toString()), testFileName -> {
        Path testFile = directoryPath.resolve(testFileName);
        URL testDataUrl = ConvertCatalog.class.getResource(testFile.toString());
        if (testDataUrl == null) {
          return;
        }

        try {
          Map<String, Object> demo = new LinkedHashMap<>();
          demo.put("schema", resource);
          demo.put("data", loadJson(ConvertCatalog.class.getResourceAsStream(testFile.toString())));
          demos.put(testFileName, demo);
          allDemos.add(testFileName);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    });

    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get("demos.json")))) {
      writer.print(objectMapper.writeValueAsString(out));
    }

    try (
        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Paths.get("allDemos.json")))) {
      writer.print(objectMapper.writeValueAsString(allDemos));
    }
  }
}
