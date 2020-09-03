package net.jimblackler.jsonschematypes.codegen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ExampleTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws IOException {
    Path out = FILE_SYSTEM.getPath("out");
    FileUtils.createOrEmpty(out);

    Path base = FILE_SYSTEM.getPath("/examples");
    scan(out, "org.example.fstab", base.resolve("fstab"));
    scan(out, "org.example.standard", base.resolve("standard").resolve("7-7-1-1"));
    scan(out, "org.example.misc", base.resolve("misc"));
    scan(out, "org.example.longread", base.resolve("longread"));
    scan(out, "org.example.meta", base.resolve("meta"));
    scan(out, "org.example.docs", base.resolve("docs"));
  }

  private static void scan(Path out, String namespace, Path testDir) throws IOException {
    new CodeGenerator(namespace).build(
        out, ExampleTest.class.getResource(testDir.resolve("schemas").toString()));
  }
}
