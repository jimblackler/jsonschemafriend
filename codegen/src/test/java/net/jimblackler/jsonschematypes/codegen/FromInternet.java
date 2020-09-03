package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschemafriend.DocumentUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
          new CodeGenerator(uri.getHost()).build(out, uri);
        }));
      }
    }
    return testsOut;
  }
}
