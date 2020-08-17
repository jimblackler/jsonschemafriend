package net.jimblackler.jsonschematypes;

import java.util.HashMap;
import java.util.Map;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;

public class Validator {
  private final SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder;

  private final Map<String, Schema> schemas = new HashMap<>();

  public Validator(String path) {
    schemaLoaderBuilder = SchemaLoader.builder()
                              .schemaClient(SchemaClient.classPathAwareClient())
                              .resolutionScope(path);
  }

  public void validate(String schemaName, Object jsonObject) {
    if (!schemas.containsKey(schemaName)) {
      // A stub is used so that one single loader (org.everit's classpath loader) can be used.
      JSONObject schemaStub = new JSONObject();
      schemaStub.put("$ref", schemaName);
      SchemaLoader schemaLoader = schemaLoaderBuilder.schemaJson(schemaStub).build();
      Schema schema = schemaLoader.load().build();
      schemas.put(schemaName, schema);
    }

    try {
      schemas.get(schemaName).validate(jsonObject);
    } catch (ValidationException ex) {
      if (jsonObject instanceof JSONObject) {
        System.out.println(((JSONObject) jsonObject).toString(2));
      } else {
        System.out.println(((JSONArray) jsonObject).toString(2));
      }

      for (String message : ex.getAllMessages()) {
        System.out.println(message);
      }
      throw new IllegalStateException(ex);
    }
  }
}