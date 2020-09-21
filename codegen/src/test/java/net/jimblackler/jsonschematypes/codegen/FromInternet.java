package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.json.JSONArray;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class FromInternet {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  @TestFactory
  Collection<DynamicTest> all() throws IOException {
    Path out = FILE_SYSTEM.getPath("out");
    FileUtils.createOrEmpty(out);

    Collection<DynamicTest> testsOut = new ArrayList<>();
    try (
        InputStream inputStream = FromInternet.class.getResourceAsStream("/internetSchemas.json")) {
      JSONArray array = new JSONArray(streamToString(inputStream));
      for (int idx = 0; idx != array.length(); idx++) {
        String str = array.getString(idx);
        testsOut.add(DynamicTest.dynamicTest(str, () -> {
          URI uri = URI.create(str);
          JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(uri.getHost());
          TypeScriptCodeGenerator typeScriptCodeGenerator = new TypeScriptCodeGenerator();
          List<CodeGenerator> generators = new ArrayList<>();
          generators.add(javaCodeGenerator);
          generators.add(typeScriptCodeGenerator);
          CodeGeneration.build(uri, new MultiGenerator(generators));
          javaCodeGenerator.output(out);
          typeScriptCodeGenerator.output(out);
        }));
      }
    }
    return testsOut;
  }
}
