package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschemafriend.DocumentUtils.loadJson;
import static net.jimblackler.jsonschematypes.ReaderUtils.getLines;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import net.jimblackler.jsonschemafriend.Ecma262Pattern;
import net.jimblackler.jsonschemafriend.FormatError;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.ValidationError;
import net.jimblackler.jsonschemafriend.ValidationException;
import net.jimblackler.jsonschemafriend.Validator;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SchemaStoreTest {
  private static final Logger LOG = Logger.getLogger(SchemaStore.class.getName());
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() {
    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");
    getLines(SuiteTest.class.getResourceAsStream(testDir.toString()), resource -> {
      Path testSchema = schemaPath.resolve(resource + ".json");
      URL resource1 = SchemaStoreTest.class.getResource(testSchema.toString());
      if (resource1 == null) {
        return;
      }

      List<DynamicTest> tests = new ArrayList<>();
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
                Object o = loadJson(SchemaStoreTest.class.getResourceAsStream(testFile.toString()));
                Schema schema = new SchemaStore(Ecma262Pattern::new, null).loadSchema(resource1);
                Collection<ValidationError> errors = new ArrayList<>();
                Validator.validate(schema, o,
                    validationError -> !(validationError instanceof FormatError), errors::add);
                if (!errors.isEmpty()) {
                  throw new ValidationException(errors);
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
