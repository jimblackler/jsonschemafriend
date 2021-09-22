package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ReaderUtils.streamToTempFile;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class ApiTest {
  @Test
  void jsonString() throws Exception {
    String schemaString = "{"
        + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\","
        + "  \"type\": \"integer\""
        + "}";

    SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
    Schema schema = schemaStore.loadSchemaJson(schemaString); // Load the schema.
    Validator validator = new Validator(); // Create a validator.
    validator.validateJson(schema, "1"); // Will not throw an exception.
    assertThrows(ValidationException.class, () -> {
      validator.validateJson(schema, "true"); // Will throw a ValidationException.
    });
  }

  @Test
  void jsonStringsFromResources() throws Exception {
    SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
    Schema schema = schemaStore.loadSchemaJson(
        StreamUtils.streamToString(ApiTest.class.getResourceAsStream("/ApiTest/schema.json")));

    Validator validator = new Validator();

    validator.validateJson(schema,
        StreamUtils.streamToString(ApiTest.class.getResourceAsStream("/ApiTest/data1.json")));

    assertThrows(ValidationException.class, () -> {
      validator.validateJson(schema,
          StreamUtils.streamToString(ApiTest.class.getResourceAsStream("/ApiTest/data2.json")));
    });
  }

  @Test
  void javaObject() throws Exception {
    JSONObject schemaJson = new JSONObject();
    schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
    schemaJson.put("type", "integer");

    SchemaStore schemaStore = new SchemaStore();
    Schema schema = schemaStore.loadSchema(schemaJson.toMap());
    Validator validator = new Validator();
    validator.validate(schema, 1);
    assertThrows(ValidationException.class, () -> { validator.validate(schema, "x"); });
  }

  @Test
  void resources() throws Exception {
    SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
    Schema schema = schemaStore.loadSchema(ApiTest.class.getResource("/ApiTest/schema.json"));

    Validator validator = new Validator();

    validator.validate(schema, ApiTest.class.getResourceAsStream("/ApiTest/data1.json"));

    assertThrows(ValidationException.class, () -> {
      validator.validate(schema, ApiTest.class.getResourceAsStream("/ApiTest/data2.json"));
    });
  }

  @Test
  void streams() throws Exception {
    SchemaStore schemaStore = new SchemaStore();
    Schema schema =
        schemaStore.loadSchema(ApiTest.class.getResourceAsStream("/ApiTest/schema.json"));

    Validator validator = new Validator();

    validator.validate(schema, ApiTest.class.getResourceAsStream("/ApiTest/data1.json"));

    assertThrows(ValidationException.class, () -> {
      validator.validate(schema, ApiTest.class.getResourceAsStream("/ApiTest/data2.json"));
    });
  }

  @Test
  void files() throws Exception {
    SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
    File file = streamToTempFile(ApiTest.class.getResourceAsStream("/ApiTest/schema.json"));
    Schema schema = schemaStore.loadSchema(file);
    Validator validator = new Validator();
    File file1 = streamToTempFile(ApiTest.class.getResourceAsStream("/ApiTest/data1.json"));
    validator.validate(schema, file1);
    assertThrows(ValidationException.class, () -> {
      File file2 = streamToTempFile(ApiTest.class.getResourceAsStream("/ApiTest/data2.json"));
      validator.validate(schema, file2);
    });
  }

  @Test
  void fromWeb() throws Exception {
    SchemaStore schemaStore = new SchemaStore();
    Schema schema =
        schemaStore.loadSchema(URI.create("https://json.schemastore.org/chrome-manifest"));

    Map<String, Object> document = new HashMap<>();
    document.put("name", "");
    document.put("version", "0.0");
    document.put("manifest_version", 2);
    new Validator().validate(schema, document);

    assertThrows(ValidationException.class,
        () -> { new Validator().validate(schema, new HashMap<String, Object>()); });
  }
}
