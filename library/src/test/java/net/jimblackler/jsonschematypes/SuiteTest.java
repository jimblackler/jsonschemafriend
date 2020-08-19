package net.jimblackler.jsonschematypes;

import static org.junit.jupiter.api.Assertions.fail;
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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SuiteTest {
  @TestFactory
  Collection<DynamicTest> suiteTest() {
    Path base = Path.of("/suites");
    return scan(base.resolve("draft7"), "http://json-schema.org/draft-07/schema#");
  }

  private static Collection<DynamicTest> scan(Path testDir, String version) {
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
            tests.add(dynamicTest(testSet.getString("description"), () -> test(version, testSet)));
          }
          System.out.println();
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }
    JSONObject outSchema = new JSONObject();
    System.out.println(outSchema.toString(2));
    return tests;
  }

  private static void test(String version, JSONObject testSet) throws GenerationException {
    if (testSet.has("schema")) {
      System.out.println(testSet.getString("description"));
      Object schema = testSet.get("schema");
      if (schema instanceof JSONObject) {
        JSONObject schema1 = (JSONObject) schema;
        schema1.put("$schema", version);
        // Sanity check with the Everit library.
        Schema everitSchema = SchemaLoader.load(schema1);
        JSONArray tests = testSet.getJSONArray("tests");
        for (int idx2 = 0; idx2 != tests.length(); idx2++) {
          JSONObject test = tests.getJSONObject(idx2);
          Object jsonObject = test.get("data");
          List<String> failures = null;
          try {
            everitSchema.validate(jsonObject);
          } catch (ValidationException ex) {
            failures = ex.getAllMessages();
          } catch (Exception e) {
            System.out.println("Schema:");
            System.out.println(schema1.toString(2));
            System.out.println();

            System.out.println("Test:");
            System.out.println(test.toString(2));
            System.out.println();

            e.printStackTrace();
            fail();
          }

          boolean valid = test.getBoolean("valid");
          if ((failures == null) != valid) {
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
            fail();
          } else {
            System.out.println(
                (valid ? "Passed" : "Failed") + " as expected: " + test.optString("description"));
          }
        }
      }

      SchemaStore schemaStore = new SchemaStore();
      schemaStore.loadBaseObject(schema);
      schemaStore.process();
      System.out.println();
    }
  }
}
