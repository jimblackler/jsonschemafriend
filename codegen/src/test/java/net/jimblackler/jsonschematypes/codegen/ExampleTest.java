package net.jimblackler.jsonschematypes.codegen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ExampleTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws IOException {
    Path base = FILE_SYSTEM.getPath("/examples");
    //    scan(base.resolve("fstab"));
    //    scan(base.resolve("standard").resolve("7-7-1-1"));
    //    scan(base.resolve("misc"));
    //    scan(base.resolve("longread"));
    //    scan(base.resolve("meta"));
    scan(base.resolve("docs"));
  }

  private static void scan(Path testDir) throws IOException {
    Path out = FILE_SYSTEM.getPath("out");
    FileUtils.createOrEmpty(out);
    CodeGenerator codeGenerator = new CodeGenerator(
        out, "org.example", ExampleTest.class.getResource(testDir.resolve("schemas").toString()));
  }
}
