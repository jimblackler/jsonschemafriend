package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ReaderUtils.getLines;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import net.jimblackler.jsonschemafriendextra.Ecma262Pattern;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class SchemaStoreTest {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicNode> all() {
    ObjectMapper objectMapper = new ObjectMapper();
    DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
    prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    ObjectWriter objectWriter = objectMapper.writer(prettyPrinter);

    Collection<DynamicNode> testsOut = new ArrayList<>();

    Path path0 = FILE_SYSTEM.getPath("/SchemaStore").resolve("src");
    Path schemaPath = path0.resolve("schemas").resolve("json");
    Path testDir = path0.resolve("test");
    Validator validator =
        new Validator(new CachedRegExPatternSupplier(Ecma262Pattern::new), validationError -> true);
    getLines(SuiteTest.class.getResourceAsStream(testDir.toString()), schemaName -> {
      Path testSchema = schemaPath.resolve(schemaName + ".json");
      URL resource1 = SchemaStoreTest.class.getResource(testSchema.toString());
      if (resource1 == null) {
        return;
      }

      Collection<DynamicTest> tests = new ArrayList<>();
      Path directoryPath = testDir.resolve(schemaName);
      InputStream resourceAsStream =
          SchemaStoreTest.class.getResourceAsStream(directoryPath.toString());
      getLines(resourceAsStream, testFileName -> {
        Path testFile = directoryPath.resolve(testFileName);
        URL testDataUrl = SchemaStoreTest.class.getResource(testFile.toString());
        if (testDataUrl == null) {
          return;
        }

        URI testSourceUri = URI.create(testDataUrl.toString());
        tests.add(DynamicTest.dynamicTest(testFileName, testSourceUri, () -> {
          Object o = objectMapper.readValue(
              SchemaStoreTest.class.getResourceAsStream(testFile.toString()), Object.class);
          Path errorsPath =
              FILE_SYSTEM.getPath("/schemaStoreErrors").resolve(schemaName).resolve(testFileName);
          InputStream src = SchemaStoreTest.class.getResourceAsStream(errorsPath.toString());
          List<String> errorReference;
          if (src == null) {
            errorReference = new ArrayList<>();
          } else {
            errorReference = objectMapper.readValue(src, List.class);
          }
          Collection<String> notReported = new LinkedHashSet<>(errorReference);
          Collection<String> extraReported = new LinkedHashSet<>();

          SchemaStore schemaStore = new SchemaStore(true);
          Schema schema = schemaStore.loadSchema(resource1);
          Map<String, Object> output = validator.validateWithOutput(schema, o);
          URI outputSchema =
              URI.create("https://json-schema.org/draft/2020-12/output/schema#/$defs/basic");
          Schema outputValidator = schemaStore.loadSchema(outputSchema, false);
          new Validator().validate(outputValidator, output);
          Collection<String> allErrors = new ArrayList<>();
          validator.validate(schema, o, error -> {
            String e = error.getUri().toString();
            if (allErrors.contains(e)) {
              return;
            }
            allErrors.add(e);
            if (!notReported.remove(e)) {
              extraReported.add(e);
            }
          });
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

          assertTrue(extraReported.isEmpty());
          assertTrue(notReported.isEmpty());
        }));
      });
      testsOut.add(
          DynamicContainer.dynamicContainer(schemaName, testSchema.toUri(), tests.stream()));
    });

    return testsOut;
  }
}
