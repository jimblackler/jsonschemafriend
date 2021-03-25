package net.jimblackler.jsonschemafriend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Catalog {
  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Collection<DynamicTest> testsOut = new ArrayList<>();

    Map<String, Object> api = DocumentUtils.loadJson(
        URI.create("https://www.schemastore.org/api/json/catalog.json").toURL().openStream());
    List<Object> schemas = (List<Object>) api.get("schemas");
    for (int idx = 0; idx != schemas.size(); idx++) {
      Map<String, Object> schema = (Map<String, Object>) schemas.get(idx);
      testsOut.add(DynamicTest.dynamicTest((String) schema.get("name"), () -> {
        URI uri = URI.create((String) schema.get("url"));
        System.out.println(uri);
        SchemaStore schemaStore = new SchemaStore();
        Schema schema1 = schemaStore.loadSchema(uri);
        System.out.println(gson.toJson(schema1.getSchemaObject()));
        Object example = schema1.getExamples();
        if (example != null) {
          System.out.println(gson.toJson(example));
        }
      }));
    }

    return testsOut;
  }
}
