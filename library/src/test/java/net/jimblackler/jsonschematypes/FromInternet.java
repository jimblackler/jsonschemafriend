package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.json.JSONArray;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class FromInternet {
  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Collection<DynamicTest> testsOut = new ArrayList<>();
    try (
        InputStream inputStream = FromInternet.class.getResourceAsStream("/internetSchemas.json")) {
      JSONArray array = new JSONArray(streamToString(inputStream));
      for (int idx = 0; idx != array.length(); idx++) {
        String str = array.getString(idx);
        testsOut.add(DynamicTest.dynamicTest(str, () -> {
          URI uri = URI.create(str);
          System.out.println(uri);
          SchemaStore schemaStore = new SchemaStore();
          schemaStore.loadSchema(uri);
        }));
      }
    }
    return testsOut;
  }
}
