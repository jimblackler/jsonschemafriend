package net.jimblackler.jsonschemafriend;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;

public class FromInternet {
  @Timeout(60)
  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Collection<DynamicTest> testsOut = new ArrayList<>();
    try (
        InputStream inputStream = FromInternet.class.getResourceAsStream("/internetSchemas.json")) {
      List<Object> array = DocumentUtils.loadJson(inputStream);
      for (Object o : array) {
        String str = (String) o;
        testsOut.add(DynamicTest.dynamicTest(str, () -> {
          Assertions.assertTimeoutPreemptively(Duration.ofSeconds(60), () -> {
            URI uri = URI.create(str);
            System.out.println(uri);
            SchemaStore schemaStore = new SchemaStore();
            schemaStore.loadSchema(uri);
          });
        }));
      }
    }
    return testsOut;
  }
}
