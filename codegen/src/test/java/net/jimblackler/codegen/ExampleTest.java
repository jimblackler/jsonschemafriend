package net.jimblackler.codegen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ExampleTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws IOException {
    Path base = FILE_SYSTEM.getPath("/examples");
    scan(base.resolve("fstab"));
    scan(base.resolve("standard").resolve("7-7-1-1"));
    scan(base.resolve("misc"));
    scan(base.resolve("longread"));
    scan(base.resolve("meta"));
  }

  private static void scan(Path testDir) throws IOException {
    Main2.outputTypes(FILE_SYSTEM.getPath("out"), "org.example",
        Main2.class.getResource(testDir.resolve("schemas").toString()));
  }
}
