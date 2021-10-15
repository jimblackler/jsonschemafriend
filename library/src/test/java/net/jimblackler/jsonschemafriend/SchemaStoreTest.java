package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ReaderUtils.getLines;
import static net.jimblackler.jsonschemafriend.ResourceUtils.getResource;
import static net.jimblackler.jsonschemafriend.ResourceUtils.getResourceAsStream;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SchemaStoreTest {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() {
    return test("test", false);
  }

  @TestFactory
  Collection<DynamicNode> allNegative() {
    return test("negative_test", true);
  }

  private Collection<DynamicNode> test(String dirName, boolean mustFail) {
    ObjectMapper objectMapper = new ObjectMapper();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    ObjectWriter objectWriter = objectMapper.writer(prettyPrinter);

    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve(dirName);
    Validator validator = new Validator();
    getLines(getResourceAsStream(SchemaStoreTest.class, testDir.toString()), schemaName -> {
      Path testSchema = schemaPath.resolve(schemaName + ".json");
      URL resource1 = getResource(SchemaStoreTest.class, testSchema.toString());
      if (resource1 == null) {
        return;
      }

      Collection<DynamicTest> tests = new ArrayList<>();
      Path directoryPath = testDir.resolve(schemaName);
      InputStream resourceAsStream =
          getResourceAsStream(SchemaStoreTest.class, directoryPath.toString());
      getLines(resourceAsStream, testFileName -> {
        Path testFile = directoryPath.resolve(testFileName);
        URL testDataUrl = getResource(SchemaStoreTest.class, testFile.toString());
        if (testDataUrl == null) {
          return;
        }

        URI testSourceUri = URI.create(testDataUrl.toString());
        tests.add(DynamicTest.dynamicTest(testFileName, testSourceUri, () -> {
          Object o = objectMapper.readValue(
              getResourceAsStream(SchemaStoreTest.class, testFile.toString()), Object.class);
          Path errorsPath =
              FILE_SYSTEM.getPath("/schemaStoreErrors").resolve(schemaName).resolve(testFileName);
          InputStream src = getResourceAsStream(SchemaStoreTest.class, errorsPath.toString());
          List<String> errorReference;
          if (src == null) {
            errorReference = new ArrayList<>();
          } else {
            errorReference = objectMapper.readValue(src, List.class);
          }
          Collection<String> notReported = new LinkedHashSet<>(errorReference);
          Collection<String> extraReported = new LinkedHashSet<>();

          SchemaStore schemaStore = new SchemaStore(true);
          Collection<String> allErrors = new ArrayList<>();
          Consumer<ValidationError> errorHandler = error -> {
            System.out.println(error);
            String e = error.getUri().toString();
            if (allErrors.contains(e)) {
              return;
            }
            allErrors.add(e);
            if (!notReported.remove(e)) {
              extraReported.add(e);
            }
          };
          Schema schema = schemaStore.loadSchema(resource1, validator, errorHandler);
          Map<String, Object> output = validator.validateWithOutput(schema, o);
          URI outputSchema =
              URI.create("https://json-schema.org/draft/2020-12/output/schema#/$defs/basic");
          Schema outputValidator = schemaStore.loadSchema(outputSchema);
          new Validator().validate(outputValidator, output, validationError -> fail());
          schema.validateExamplesRecursive(validator, errorHandler);
          validator.validate(schema, o, errorHandler);
          if (mustFail) {
            assertFalse(allErrors.isEmpty(), "No errors reported in must-fail test");
          }

          if (!mustFail) {
            if (new File("schemaStoreErrors").exists()) {
              if (allErrors.isEmpty()) {
                errorsPath.toFile().delete();
              } else {
                Path schemaStoreErrors = FILE_SYSTEM.getPath("schemaStoreErrors");
                Files.createDirectories(schemaStoreErrors.resolve(schemaName));
                try (FileWriter w = new FileWriter(
                         schemaStoreErrors.resolve(schemaName).resolve(testFileName).toString())) {
                  objectWriter.writeValue(w, allErrors);
                }
              }
            }

            System.out.println(objectWriter.writeValueAsString(output));

            assertTrue(extraReported.isEmpty(), "Errors reported not seen in reference file");
            assertTrue(notReported.isEmpty(), "Errors in reference file not reported");
          }
        }));
      });
      testsOut.add(
          DynamicContainer.dynamicContainer(schemaName, testSchema.toUri(), tests.stream()));
    });

    return testsOut;
  }
}
