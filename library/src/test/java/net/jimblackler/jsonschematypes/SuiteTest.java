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
    Path own = Path.of("/suites").resolve("own");
    return scan(own, own.resolve("remotes"), URI.create("http://json-schema.org/draft-07/schema#"),
        true, false);
  }

  @TestFactory
  DynamicNode draft3Own() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft3"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-03/schema#"), false, false);
  }
  @TestFactory
  DynamicNode draft4() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft4"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-04/schema#"), true, false);
  }

  @TestFactory
  DynamicNode draft4Own() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft4"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-04/schema#"), false, false);
  }

  @TestFactory
  DynamicNode backwardsCompatibilitySpecialTest() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft4"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-07/schema#"), false, false);
  }

  @TestFactory
  DynamicNode draft6() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft6"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-06/schema#"), true, false);
  }

  @TestFactory
  DynamicNode draft6Own() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft6"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-06/schema#"), false, false);
  }

  @TestFactory
  DynamicNode draft7() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft7"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-07/schema#"), true, false);
  }

  @TestFactory
  DynamicNode draft7Own() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft7"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-07/schema#"), false, false);
  }

  @TestFactory
  DynamicNode draft2019_09() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft2019-09"), jsts.resolve("remotes"),
        URI.create("https://json-schema.org/draft/2019-09/schema"), true, false);
  }

  @TestFactory
  DynamicNode draft2019_09own() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft2019-09"), jsts.resolve("remotes"),
        URI.create("https://json-schema.org/draft/2019-09/schema"), false, false);
  }

  @TestFactory
  DynamicNode draft7SchemaOnly() {
    Path jsts = Path.of("/suites").resolve("jsts");
    return scan(jsts.resolve("tests").resolve("draft7"), jsts.resolve("remotes"),
        URI.create("http://json-schema.org/draft-07/schema#"), true, true);
  }

  private static DynamicNode scan(
      Path testDir, Path remotes, URI metaSchema, boolean everit, boolean schemaOnly) {
    Collection<DynamicNode> allFileTests = new ArrayList<>();
    return dirScan(testDir, remotes, metaSchema, everit, schemaOnly, allFileTests);
  }

  private static DynamicNode dirScan(Path testDir, Path remotes, URI metaSchema, boolean everit,
      boolean schemaOnly, Collection<DynamicNode> allFileTests) {
    try (InputStream inputStream = ExampleTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;
      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          allFileTests.add(
              jsonTestFile(testDir, remotes, resource, metaSchema, everit, schemaOnly));
        } else {
          if (false) // hack to exclude 'optional'
            dirScan(
                testDir.resolve(resource), remotes, metaSchema, everit, schemaOnly, allFileTests);
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }
    return dynamicContainer(testDir.toString(), allFileTests);
  }

  private static DynamicNode jsonTestFile(Path testDir, Path remotes, String resource,
      URI metaSchema, boolean everit, boolean schemaOnly) throws GenerationException {
    Collection<DynamicNode> nodes = new ArrayList<>();
    JSONArray data = (JSONArray) Utils.getJsonObject(testDir.resolve(resource).toString());
    for (int idx = 0; idx != data.length(); idx++) {
      JSONObject testSet = data.getJSONObject(idx);
      if (!testSet.has("schema")) {
        continue; // ever happens?
      }
      nodes.add(singleSchemaTest(testSet, remotes, metaSchema, everit, schemaOnly));
    }
    return dynamicContainer(resource, nodes);
  }

  private static DynamicNode singleSchemaTest(
      JSONObject testSet, Path remotes, URI metaSchema, boolean everit, boolean schemaOnly) {
    Collection<DynamicTest> everitTests = new ArrayList<>();
    Collection<DynamicTest> ownTests = new ArrayList<>();
    Object schema = testSet.get("schema");
    URL resource = SuiteTest.class.getResource(remotes.toString());
    {
      if (schema instanceof JSONObject) {
        JSONObject schema1 = (JSONObject) schema;
        schema1.put("$schema", metaSchema.toString());
      }
      JSONArray tests1 = testSet.getJSONArray("tests");
      for (int idx2 = 0; idx2 != tests1.length(); idx2++) {
        JSONObject test = tests1.getJSONObject(idx2);
        Object data = test.get("data");
        boolean valid = test.getBoolean("valid");
        String description = test.optString("description", data + (valid ? " succeeds" : " fails"));

        if (!schemaOnly) {
          ownTests.add(dynamicTest(description, () -> {
            System.out.println("Schema:");
            if (schema instanceof JSONObject) {
              System.out.println(((JSONObject) schema).toString(2));
            } else {
              System.out.println(schema);
            }
            System.out.println();

            DocumentSource documentSource = new DocumentSource(List.of(in
                -> URI.create(
                    in.toString().replace("http://localhost:1234", resource.toString()))));
            URI local = new URI("memory", "local", null, null);
            documentSource.store(local, schema);
            SchemaStore schemaStore = new SchemaStore(documentSource);
            net.jimblackler.jsonschematypes.Schema schema1 =
                schemaStore.getSchema(local, URI.create("http://json-schema.org/draft-07/schema#"));

            System.out.println("Test:");
            System.out.println(test.toString(2));
            System.out.println();

            List<ValidationError> errors = new ArrayList<>();
            schema1.validate(data, URI.create(""), errors::add);

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
        if (schema instanceof JSONObject) {
          if (everit) {
            everitTests.add(dynamicTest(description, () -> {
              JSONObject schema1 = (JSONObject) schema;
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

              List<String> failures = null;
              try {
                everitSchema.validate(data);
              } catch (ValidationException ex) {
                System.out.println(ex.toJSON().toString());
                failures = ex.getAllMessages();
              } catch (Exception e) {
                fail(e);
              }

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
      }
    }
    if (schemaOnly) {
      ownTests.add(dynamicTest("schema", () -> {
        System.out.println("Schema:");
        if (schema instanceof JSONObject) {
          System.out.println(((JSONObject) schema).toString(2));
        } else {
          System.out.println(schema);
        }
        System.out.println();

        DocumentSource documentSource = new DocumentSource(List.of(
            in -> URI.create(in.toString().replace("http://localhost:1234", resource.toString()))));
        SchemaStore schemaStore = new SchemaStore(documentSource);
        URI local = new URI("memory", "local", null, null);
        documentSource.store(local, schema);
        schemaStore.validateAndGet(local);
      }));
    }
    if (everit) {
      List<DynamicContainer> dynamicNodes =
          List.of(dynamicContainer("everit", everitTests), dynamicContainer("own", ownTests));
      return dynamicContainer(testSet.getString("description"), dynamicNodes);
    } else {
      return dynamicContainer(testSet.getString("description"), ownTests);
    }
  }
}
