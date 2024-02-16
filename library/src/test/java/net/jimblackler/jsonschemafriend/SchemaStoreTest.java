package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ReaderUtils.getLines;
import static net.jimblackler.jsonschemafriend.ResourceUtils.getResource;
import static net.jimblackler.jsonschemafriend.ResourceUtils.getResourceAsStream;
import static net.jimblackler.jsonschemafriend.TestUtil.clearDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * Run through the test cases setup in the
 * <a href="https://github.com/SchemaStore/schemastore">SchemaStore</a> repo.
 *
 * <p>These tests are out of our control. New tests are constantly being added and can cause failures locally.
 * To support a stable build, only known good test cases are run as part of the CI build.
 * The list of known good test cases can be updated by setting {@code WRITE_OUTPUT} to {@code true} below,
 * running the tests and then reviewing & then replacing {@code library/src/test/resources/schemaStorePasses}
 * with {@code library/schemaStorePasses}.
 */
public class SchemaStoreTest {
  private static final FileSystem FILE_SYSTEM = FileSystems.getDefault();
  private static final Path ERRORS_OUTPUT_DIR = Paths.get("schemaStoreErrors");
  private static final Path PASSES_OUTPUT_DIR = Paths.get("schemaStorePasses");
  /**
   * Locally change this to {@code true} to have the test write out details of passing a failing tests
   */
  private static final boolean WRITE_OUTPUT = false;
  /**
   * GitHub workflow that runs with this set to {@code true}, so that only known good tests are run.
   *
   * <p>This stops bad test data in the SchemaStore project from causing build failures in this project.
   */
  private static final boolean SMOKE_TEST = Boolean.getBoolean("run.smoke.test");

  @BeforeAll
  static void classSetUp() throws Exception {
      if (WRITE_OUTPUT) {
        clearDirectory(ERRORS_OUTPUT_DIR);
        clearDirectory(PASSES_OUTPUT_DIR);
      }
  }

  @Test
  void shouldNotCheckInWithWriteOutputEnabled() {
    assertFalse(WRITE_OUTPUT);
  }

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
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
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
          if (SMOKE_TEST) {
            Path passes = FILE_SYSTEM.getPath("/schemaStorePasses").resolve(mustFail ? "failing" : "passing").resolve(schemaName).resolve(testFileName);
            assumeTrue(getResource(SchemaStoreTest.class, passes.toString()) != null);
          }

          ObjectMapper mapper = null;
          if (testFile.toString().endsWith(".json")) {
            mapper = objectMapper;
          }
          if (testFile.toString().endsWith(".yml") || testFile.toString().endsWith(".yaml")) {
            mapper = yamlMapper;
          }

          // Unsupported test file format if null:
          assumeTrue(mapper != null);

          Object o = mapper.readValue(
              getResourceAsStream(SchemaStoreTest.class, testFile.toString()), Object.class);
          Path errorsPath =
              FILE_SYSTEM.getPath("/schemaStoreErrors").resolve(schemaName).resolve(testFileName);
          InputStream src = getResourceAsStream(SchemaStoreTest.class, errorsPath.toString());
          List<String> errorReference;
          if (src == null) {
            errorReference = new ArrayList<>();
          } else {
            errorReference = objectMapper.readValue(src, new TypeReference<List<String>>() {});
          }
          Collection<String> notReported = new LinkedHashSet<>(errorReference);
          Collection<String> extraReported = new LinkedHashSet<>();

          SchemaStore schemaStore = new SchemaStore(true);
          List<String> allErrors = new ArrayList<>();
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
            maybeWriteErrorsFile(objectWriter, schemaName, testFileName, allErrors);

            System.out.println(objectWriter.writeValueAsString(output));

            assertThat("Errors reported not seen in reference file",  extraReported, is(empty()));
            assertThat("Errors in reference file not reported", notReported, is(empty()));
          }

          maybeWriteOutPassFile(mustFail, schemaName, testFileName);
        }));
      });
      testsOut.add(
          DynamicContainer.dynamicContainer(schemaName, testSchema.toUri(), tests.stream()));
    });

    return testsOut;
  }

  private static void maybeWriteErrorsFile(
          ObjectWriter objectWriter,
          String schemaName,
          String testFileName,
          List<String> allErrors) throws IOException {
    if (!WRITE_OUTPUT) {
      return;
    }

    Path schemaDir = ERRORS_OUTPUT_DIR.resolve(schemaName);
    Files.createDirectories(schemaDir);
    allErrors.sort(Comparator.naturalOrder());
    objectWriter.writeValue(schemaDir.resolve(testFileName).toFile(), allErrors);
  }

  private void maybeWriteOutPassFile(
          boolean mustFail,
          String schemaName,
          String testFileName) throws IOException  {
    if (!WRITE_OUTPUT) {
      return;
    }

    Path schemaDir = PASSES_OUTPUT_DIR.resolve(mustFail ? "failing" : "passing").resolve(schemaName);
    Files.createDirectories(schemaDir);
    Files.createFile(schemaDir.resolve(testFileName));
  }
}
