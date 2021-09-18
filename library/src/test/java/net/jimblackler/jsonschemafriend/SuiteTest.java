package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.Utils.getOrDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.jimblackler.jsonschemafriendextra.Ecma262Pattern;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();
  public static final boolean OPTIONAL_TESTS = false;

  private static Collection<DynamicNode> scan(
      Iterable<Path> testDirs, Path remotes, URI metaSchema) {
    Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    Collection<DynamicNode> allFileTests = new ArrayList<>();
    URL resource1 = SuiteTest.class.getResource(remotes.toString());
    UrlRewriter urlRewriter =
        in -> URI.create(in.toString().replace("http://localhost:1234", resource1.toString()));
    RegExPatternSupplier supplier = true ? Ecma262Pattern::new : JavaRegExPattern::new;
    Validator validator =
        new Validator(new CachedRegExPatternSupplier(supplier), validationError -> true);
    for (Path testDir : testDirs) {
      Collection<DynamicNode> dirTests = new ArrayList<>();
      try (InputStream inputStream = SuiteTest.class.getResourceAsStream(testDir.toString());
           BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (!resource.endsWith(".json")) {
            continue;
          }
          Collection<DynamicNode> nodes = new ArrayList<>();
          Path resourcePath = testDir.resolve(resource);
          URI testSourceUri = SuiteTest.class.getResource(resourcePath.toString()).toURI();
          try (InputStream inputStream1 =
                   SuiteTest.class.getResourceAsStream(resourcePath.toString())) {
            List<Object> data = DocumentUtils.loadJson(inputStream1);
            for (int idx = 0; idx != data.size(); idx++) {
              Map<String, Object> testSet = (Map<String, Object>) data.get(idx);
              if (!testSet.containsKey("schema")) {
                continue; // ever happens?
              }

              Collection<DynamicTest> tests = new ArrayList<>();
              Object schemaObject = testSet.get("schema");
              if (schemaObject instanceof Map) {
                Map<String, Object> schema1 = (Map<String, Object>) schemaObject;
                if (!schema1.containsKey("$schema")) {
                  schema1.put("$schema", metaSchema.toString());
                }
              }
              List<Object> tests1 = (List<Object>) testSet.get("tests");
              for (int idx2 = 0; idx2 != tests1.size(); idx2++) {
                Map<String, Object> test = (Map<String, Object>) tests1.get(idx2);
                Object data1 = test.get("data");
                boolean valid = getOrDefault(test, "valid", false);
                String description = getOrDefault(test, "description", "Data: " + data1);
                if (!valid) {
                  description += " (F)";
                }

                tests.add(dynamicTest(description, testSourceUri, () -> {
                  System.out.println("Schema:");
                  System.out.println(gson.toJson(schemaObject));
                  System.out.println();

                  SchemaStore schemaStore = new SchemaStore(urlRewriter, true);
                  net.jimblackler.jsonschemafriend.Schema schema1 =
                      schemaStore.loadSchema(schemaObject);

                  System.out.println("Test:");
                  System.out.println(gson.toJson(test));
                  System.out.println();

                  List<ValidationError> errors = new ArrayList<>();
                  validator.validate(schema1, data1, URI.create(""), errors::add);

                  System.out.print("Expected to " + (valid ? "pass" : "fail") + " ... ");
                  if (errors.isEmpty()) {
                    System.out.println("Passed");
                  } else {
                    System.out.println("Failures:");
                    for (ValidationError error : errors) {
                      System.out.println(error);
                    }
                    System.out.println();
                  }

                  assertEquals(errors.isEmpty(), valid);
                }));
              }
              nodes.add(dynamicContainer(
                  (String) testSet.get("description"), testSourceUri, tests.stream()));
            }
          }
          dirTests.add(dynamicContainer(resource, testSourceUri, nodes.stream()));
        }
        allFileTests.add(
            dynamicContainer(testDir.getName(testDir.getNameCount() - 1).toString(), dirTests));
      } catch (IOException | URISyntaxException e) {
        throw new IllegalStateException(e);
      }
    }

    return allFileTests;
  }

  private static Collection<DynamicNode> test(String set, String metaSchema) {
    Path suite = FILE_SYSTEM.getPath("/suites").resolve("JSON-Schema-Test-Suite");
    Path tests = suite.resolve("tests").resolve(set);
    Path optional = tests.resolve("optional");
    List<Path> paths = new ArrayList<>();
    paths.add(tests);
    if (OPTIONAL_TESTS) {
      paths.add(optional);
      paths.add(optional.resolve("format"));
    }
    Path remotes = suite.resolve("remotes");
    return scan(paths, remotes, URI.create(metaSchema));
  }

  @TestFactory
  Collection<DynamicNode> own() {
    Path path = FILE_SYSTEM.getPath("/suites");
    Path own = path.resolve("own");
    Collection<Path> testDirs = new HashSet<>();
    testDirs.add(own);
    Path remotes = path.resolve("own_remotes");
    URI metaSchema = URI.create("http://json-schema.org/draft-07/schema#");
    return scan(testDirs, remotes, metaSchema);
  }

  @TestFactory
  Collection<DynamicNode> draft3() {
    return test("draft3", "http://json-schema.org/draft-03/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft4() {
    return test("draft4", "http://json-schema.org/draft-04/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft6() {
    return test("draft6", "http://json-schema.org/draft-06/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft7() {
    return test("draft7", "http://json-schema.org/draft-07/schema#");
  }

  @TestFactory
  Collection<DynamicNode> draft2019_09() {
    return test("draft2019-09", "https://json-schema.org/draft/2019-09/schema");
  }

  @TestFactory
  Collection<DynamicNode> draft2020_12() {
    return test("draft2020-12", "https://json-schema.org/draft/2020-12/schema");
  }
}
