package net.jimblackler.jsonschematypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
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
  DynamicNode own() {
    Path suites = Path.of("/suites");
    return scan(suites.resolve("own"), Path.of(""),
        "http://json-schema.org/draft-07/schema#");
  }

  @TestFactory
  DynamicNode draft4() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft4"), jsts.resolve("remotes"),
        "http://json-schema.org/draft-04/schema#");
  }

  @TestFactory
  DynamicNode draft6() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft6"), jsts.resolve("remotes"),
        "http://json-schema.org/draft-06/schema#");
  }

  @TestFactory
  DynamicNode draft7() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft7"), jsts.resolve("remotes"),
            "http://json-schema.org/draft-07/schema#");
  }

  @TestFactory
  DynamicNode draft2019_09() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft2019-09"), jsts.resolve("remotes"),
        "https://json-schema.org/draft/2019-09/schema#");
  }

  private static DynamicNode scan(Path testDir, Path remotes, String version) {
    Collection<DynamicNode> allFileTests = new ArrayList<>();
    try (InputStream inputStream = ExampleTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;
      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          allFileTests.add(jsonTestFile(testDir, remotes, resource, version));
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }
    return dynamicContainer(testDir.toString(), allFileTests);
  }

  private static DynamicNode jsonTestFile(
      Path testDir, Path remotes, String resource, String version) throws GenerationException {
    Collection<DynamicNode> nodes = new ArrayList<>();
    JSONArray data = (JSONArray) Utils.getJsonObject(testDir.resolve(resource).toString());
    for (int idx = 0; idx != data.length(); idx++) {
      JSONObject testSet = data.getJSONObject(idx);
      if (!testSet.has("schema")) {
        continue; // ever happens?
      }
      nodes.add(singleSchemaTest(testSet, remotes, version));
    }
    return dynamicContainer(resource, nodes);
  }

  private static DynamicNode singleSchemaTest(JSONObject testSet, Path remotes, String version) {
    Collection<DynamicTest> everitTests = new ArrayList<>();
    Object schema = testSet.get("schema");
    URL resource = SuiteTest.class.getResource(remotes.toString());
    if (schema instanceof JSONObject) {
      JSONObject schema1 = (JSONObject) schema;
      schema1.put("$schema", version);
      JSONArray tests1 = testSet.getJSONArray("tests");
      for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
        JSONObject test = tests1.getJSONObject(idx2);
        everitTests.add(dynamicTest(test.optString("description", "No description"), () -> {
          System.out.println("Schema:");
          System.out.println(schema1.toString(2));
          System.out.println();

          Schema everitSchema = SchemaLoader.load(schema1, url -> {
            url = url.replace("http://localhost:1234", resource.toString());
            try {
              return new URL(url).openStream();
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });

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
    }
    return dynamicContainer(testSet.getString("description"),
        List.of(dynamicContainer("everit", everitTests), dynamicTest("own", () -> {
          System.out.println("Schema:");
          if (schema instanceof JSONObject) {
            System.out.println(((JSONObject) schema).toString(2));
          } else {
            System.out.println(schema);
          }
          System.out.println();

          SchemaStore schemaStore = new SchemaStore();
          schemaStore.addRewriter(in
              -> URI.create(in.toString().replace("http://localhost:1234", resource.toString())));
          schemaStore.loadBaseObject(schema);
          schemaStore.process();
        })));
  }
}
