package net.jimblackler.jsonschematypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  @TestFactory
  Collection<DynamicContainer> t2() {
    Collection<DynamicContainer> container = new ArrayList<>();
    return List.of(dynamicContainer("inner1", List.of(dynamicTest("t", () -> fail()))));
  }

  @TestFactory
  Collection<DynamicContainer> own() {
    return scan(Path.of("/suites").resolve("own"), "http://json-schema.org/draft-07/schema#");
  }

  @TestFactory
  Collection<DynamicContainer> draft7() {
    return scan(Path.of("/suites").resolve("draft7"), "http://json-schema.org/draft-07/schema#");
  }

  private static Collection<DynamicContainer> scan(Path testDir, String version) {
    List<DynamicNode> everit = new ArrayList<>();
    List<DynamicNode> own = new ArrayList<>();

    System.out.println();
    System.out.println(testDir);
    System.out.println();

    Collection<DynamicTest> tests = new ArrayList<>();
    try (InputStream inputStream = ExampleTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;

      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          System.out.println(resource);
          JSONArray data = (JSONArray) Utils.getJsonObject(testDir.resolve(resource).toString());
          for (int idx = 0; idx != data.length(); idx++) {
            JSONObject testSet = data.getJSONObject(idx);
            if (testSet.has("schema")) {
              System.out.println(testSet.getString("description"));
              Object schema = testSet.get("schema");
              if (schema instanceof JSONObject) {
                JSONObject schema1 = (JSONObject) schema;
                schema1.put("$schema", version);

                JSONArray tests1 = testSet.getJSONArray("tests");
                List<DynamicTest> subTests = new ArrayList<>();

                for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
                  JSONObject test = tests1.getJSONObject(idx2);
                  subTests.add(dynamicTest(test.getString("description"), () -> {
                    System.out.println("Schema:");
                    System.out.println(schema1.toString(2));
                    System.out.println();

                    Schema everitSchema = SchemaLoader.load(schema1);

                    System.out.println("Test:");
                    System.out.println(test.toString(2));
                    System.out.println();

                    Object jsonObject = test.get("data");
                    List<String> failures = null;
                    try {
                      everitSchema.validate(jsonObject);
                    } catch (ValidationException ex) {
                      System.out.println(ex.toJSON().toString());
                      failures = ex.getAllMessages();
                    } catch (Exception e) {
                      fail(e);
                    }
                    boolean valid = test.getBoolean("valid");

                    System.out.println("Schema:");
                    System.out.println(schema1.toString(2));
                    System.out.println();

                    System.out.println("Test:");
                    System.out.println(test.toString(2));
                    System.out.println();

                    if (failures != null) {
                      System.out.println("Failures:");
                      for (String message : failures) {
                        System.out.println(message);
                      }
                      System.out.println();
                    }

                    System.out.print("Expeced to " + (valid ? "pass" : "fail") + " ... ");
                    System.out.println((failures == null ? "passed" : "failed"));

                    assertEquals(failures == null, valid);
                  }));
                }
                everit.add(dynamicContainer(testSet.getString("description"), subTests));
              }

              own.add(dynamicTest(testSet.getString("description"), () -> {
                SchemaStore schemaStore = new SchemaStore();
                schemaStore.loadBaseObject(schema);
                schemaStore.process();
                System.out.println();
              }));
            }
          }
          System.out.println();
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }
    JSONObject outSchema = new JSONObject();
    System.out.println(outSchema.toString(2));

    return List.of(dynamicContainer("everit", everit), dynamicContainer("own", own));
  }
}
