package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;

public class SuiteTest {
  public static void main(String[] args) {
    Path base = Path.of("/suites");
    scan(base.resolve("own"), "http://json-schema.org/draft-07/schema#");
    scan(base.resolve("draft7"), "http://json-schema.org/draft-07/schema#");
  }

  private static void scan(Path testDir, String version) {
    System.out.println();
    System.out.println(testDir);
    System.out.println();

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
                  } else {
                    System.out.println((valid ? "Passed" : "Failed")
                        + " as expected: " + test.optString("description"));
                  }
                }
              }

              SchemaStore schemaStore = new SchemaStore();
              schemaStore.loadBaseObject(schema);
              schemaStore.process();
              System.out.println();
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
  }
}
