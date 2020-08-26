package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class FromInternet {
  @TestFactory
  Collection<DynamicTest> all() throws GenerationException {
    Collection<DynamicTest> testsOut = new ArrayList<>();
    JSONArray array = (JSONArray) Utils.getJsonObject("/internetSchemas.json");
    for (int idx = 0; idx != array.length(); idx++) {
      String str = array.getString(idx);
      testsOut.add(DynamicTest.dynamicTest(str, () -> {
        URI uri = URI.create(str);
        System.out.println(uri);
        DocumentSource documentSource = new DocumentSource(List.of());
        SchemaStore schemaStore = new SchemaStore(documentSource);

        schemaStore.validateAndGet(uri, URI.create("http://json-schema.org/draft-07/schema#"));
      }));
    }
    return testsOut;
  }
}
