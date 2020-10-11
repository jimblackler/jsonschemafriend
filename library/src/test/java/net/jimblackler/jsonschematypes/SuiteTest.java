package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschemafriend.ObjectFixer.rewriteObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.UrlRewriter;
import net.jimblackler.jsonschemafriend.ValidationError;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  private static Collection<DynamicNode> scan(
      Iterable<Path> testDirs, Path remotes, URI metaSchema) {
    Collection<DynamicNode> allFileTests = new ArrayList<>();
    URL resource1 = SuiteTest.class.getResource(remotes.toString());
    UrlRewriter urlRewriter =
        in -> URI.create(in.toString().replace("http://localhost:1234", resource1.toString()));
    Validator validator = new Validator();
    for (Path testDir : testDirs) {
      try (InputStream inputStream = SuiteTest.class.getResourceAsStream(testDir.toString());
           BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (!resource.endsWith(".json")) {
            continue;
          }
          Collection<DynamicNode> nodes = new ArrayList<>();
          try (InputStream inputStream1 =
                   SuiteTest.class.getResourceAsStream(testDir.resolve(resource).toString())) {
            JSONArray data = (JSONArray) DocumentUtils.loadJson(inputStream1);
            for (int idx = 0; idx != data.length(); idx++) {
              JSONObject testSet = data.getJSONObject(idx);
              if (!testSet.has("schema")) {
                continue; // ever happens?
              }

              Collection<DynamicTest> tests = new ArrayList<>();
              Object schemaObject = testSet.get("schema");
              if (schemaObject instanceof JSONObject) {
                JSONObject schema1 = (JSONObject) rewriteObject(schemaObject);
                schema1.put("$schema", metaSchema.toString());
              }
              JSONArray tests1 = testSet.getJSONArray("tests");
              for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
                JSONObject test = tests1.getJSONObject(idx2);
                Object data1 = rewriteObject(test.get("data"));
                boolean valid = test.getBoolean("valid");
                String description = test.optString("description", "Data: " + data1);
                if (!valid) {
                  description += " (F)";
                }

                tests.add(dynamicTest(description, () -> {
                  System.out.println("Schema:");
                  if (schemaObject instanceof JSONObject) {
                    System.out.println(((JSONObject) schemaObject).toString(2));
                  } else {
                    System.out.println(schemaObject);
                  }
                  System.out.println();

                  SchemaStore schemaStore = new SchemaStore(urlRewriter);
                  net.jimblackler.jsonschemafriend.Schema schema1 =
                      schemaStore.loadSchema(schemaObject);

                  System.out.println("Test:");
                  System.out.println(test.toString(2));
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
              nodes.add(dynamicContainer(testSet.getString("description"), tests));
            }
          }
          allFileTests.add(dynamicContainer(resource, nodes));
        }
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }

    return allFileTests;
  }

  private static Collection<DynamicNode> test(String set, String metaSchema) {
    Path suite = FILE_SYSTEM.getPath("/suites").resolve("JSON-Schema-Test-Suite");
    Path tests = suite.resolve("tests").resolve(set);
    Path optional = tests.resolve("optional");
    List<Path> paths = List.of(tests, optional, optional.resolve("format"));
    Path remotes = suite.resolve("remotes");
    return scan(paths, remotes, URI.create(metaSchema));
  }

  @TestFactory
  Collection<DynamicNode> own() {
    Path path = FILE_SYSTEM.getPath("/suites");
    Path own = path.resolve("own");
    Collection<Path> testDirs = List.of(own);
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
}
