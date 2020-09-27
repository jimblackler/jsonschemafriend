package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class Catalog {
  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Collection<DynamicTest> testsOut = new ArrayList<>();

    JSONObject api = new JSONObject(streamToString(
        URI.create("https://www.schemastore.org/api/json/catalog.json").toURL().openStream()));
    JSONArray schemas = api.getJSONArray("schemas");
    for (int idx = 0; idx != schemas.length(); idx++) {
      JSONObject schema = schemas.getJSONObject(idx);
      testsOut.add(DynamicTest.dynamicTest(schema.getString("name"), () -> {
        URI uri = URI.create(schema.getString("url"));
        System.out.println(uri);
        SchemaStore schemaStore = new SchemaStore();
        Schema schema1 = schemaStore.loadSchema(uri);
        System.out.println(DocumentUtils.toString(schema1.getSchemaObject()));

        Object example = schema1.getExample();
        if (example != null) {
          System.out.println(DocumentUtils.toString(example));
        }
      }));
    }

    return testsOut;
  }
}
