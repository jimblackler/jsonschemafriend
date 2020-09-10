package net.jimblackler.jsonschematypes.codegen;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ExampleTest {
  public static final FileSystem FILE_SYSTEM = FileSystems.getDefault();

  public static void main(String[] args) throws IOException, CodeGenerationException {
    Path out = FILE_SYSTEM.getPath("out");
    FileUtils.createOrEmpty(out);

    Path base = FILE_SYSTEM.getPath("/examples");
    scan(out, "org.example.fstab", base, "fstab");
    scan(out, "org.example.standard", base, ("standard/7-7-1-1"));
    scan(out, "org.example.misc", base, "misc");
    scan(out, "org.example.longread", base, "longread");
    scan(out, "org.example.meta", base, "meta");
    scan(out, "org.example.docs", base, "docs");
  }

  private static void scan(Path out, String namespace, Path base, String append)
      throws CodeGenerationException, IOException {
    Path testDir = base.resolve(append);
    JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(namespace);
    TypeScriptCodeGenerator typeScriptCodeGenerator = new TypeScriptCodeGenerator();
    List<CodeGenerator> generators = new ArrayList<>();
    generators.add(javaCodeGenerator);
    generators.add(typeScriptCodeGenerator);
    CodeGeneration.build(ExampleTest.class.getResource(testDir.resolve("schemas").toString()),
        new MultiGenerator(generators));
    javaCodeGenerator.output(out);
    typeScriptCodeGenerator.output(out.resolve(append));
  }
}
