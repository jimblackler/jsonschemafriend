package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

public class SchemaStoreTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() throws IOException {
    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");
    try (InputStream inputStream = SuiteTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;
      while ((resource = bufferedReader.readLine()) != null) {
        String finalResource = resource;
        List<DynamicTest> tests = new ArrayList<>();
        Path filePath = testDir.resolve(finalResource);
        try (InputStream inputStream2 =
                 SchemaStoreTest.class.getResourceAsStream(filePath.toString());
             BufferedReader bufferedReader2 =
                 new BufferedReader(new InputStreamReader(inputStream2, StandardCharsets.UTF_8))) {
          String resource2;
          while ((resource2 = bufferedReader2.readLine()) != null) {
            String finalResource1 = resource2;
            tests.add(DynamicTest.dynamicTest(resource2, () -> {
              Object o = DocumentUtils.loadJson(SchemaStoreTest.class.getResourceAsStream(
                  filePath.resolve(finalResource1).toString()));
              SchemaStore schemaStore = new SchemaStore();
              Schema schema = schemaStore.loadSchema(SchemaStoreTest.class.getResource(
                  schemaPath.resolve(finalResource + ".json").toString()));
              Validator.validate(schema, o);
            }));
          }
        }
        testsOut.add(DynamicContainer.dynamicContainer(resource, tests));
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    return testsOut;
  }
}
