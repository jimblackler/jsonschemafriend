package net.jimblackler.jsonschemafriend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ValidationOutputTest {
  @Test
  void resources() throws Exception {
    SchemaStore schemaStore = new SchemaStore();
    Map<String, Object> schemaObject = new HashMap<>();
    schemaObject.put("$schema", "https://json-schema.org/draft/2020-12/schema");
    schemaObject.put("type", "string");

    Schema schema = schemaStore.loadSchema(schemaObject);

    Validator validator = new Validator();

    {
      Object output = validator.validateWithOutput(schema, 1).get("valid");
      checkOutput(schemaStore, output);
      assertNotEquals(Boolean.TRUE, output);
    }
    {
      Map<String, Object> output = validator.validateWithOutput(schema, "x");
      checkOutput(schemaStore, output);
      assertEquals(Boolean.TRUE, output.get("valid"));
    }
  }

  private void checkOutput(SchemaStore schemaStore, Object output)
      throws GenerationException, ValidationException {
    URI outputSchema =
        URI.create("https://json-schema.org/draft/2020-12/output/schema#/$defs/basic");
    Schema outputValidator = schemaStore.loadSchema(outputSchema, null);
    new Validator().validate(outputValidator, output);
  }
}
