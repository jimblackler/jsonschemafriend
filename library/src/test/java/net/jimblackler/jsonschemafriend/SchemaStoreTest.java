package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ReaderUtils.getLines;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jimblackler.jsonschemafriendextra.Ecma262Pattern;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SchemaStoreTest {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() {
    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");
    Validator validator =
        new Validator(new CachedRegExPatternSupplier(Ecma262Pattern::new), validationError -> true);
    getLines(SuiteTest.class.getResourceAsStream(testDir.toString()), resource -> {
      Path testSchema = schemaPath.resolve(resource + ".json");
      URL resource1 = SchemaStoreTest.class.getResource(testSchema.toString());
      if (resource1 == null) {
        return;
      }

      Collection<DynamicTest> tests = new ArrayList<>();
      Path directoryPath = testDir.resolve(resource);
      getLines(
          SchemaStoreTest.class.getResourceAsStream(directoryPath.toString()), testFileName -> {
            Path testFile = directoryPath.resolve(testFileName);
            URL testDataUrl = SchemaStoreTest.class.getResource(testFile.toString());
            if (testDataUrl == null) {
              return;
            }

            try {
              tests.add(DynamicTest.dynamicTest(testFileName, testDataUrl.toURI(), () -> {
                Object o = new ObjectMapper().readValue(
                    SchemaStoreTest.class.getResourceAsStream(testFile.toString()), Object.class);
                SchemaStore schemaStore = new SchemaStore(true);
                Schema schema = schemaStore.loadSchema(resource1);
                Map<String, Object> output = validator.validateWithOutput(schema, o);
                URI outputSchema =
                    URI.create("https://json-schema.org/draft/2020-12/output/schema#/$defs/basic");
                Schema outputValidator = schemaStore.loadSchema(
                    outputSchema, false);
                new Validator().validate(outputValidator, output);
                if (!(Boolean) output.get("valid")) {
                  throw new StandardValidationException(output);
                }
              }));
            } catch (URISyntaxException e) {
              throw new IllegalStateException(e);
            }
          });
      testsOut.add(DynamicContainer.dynamicContainer(resource, testSchema.toUri(), tests.stream()));
    });

    return testsOut;
  }
}
