package net.jimblackler.jsonschematypes.codegen;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;

public class JavaCodeGenerator implements CodeGenerator {
  private final Map<URI, JavaBuilder> builtClasses = new HashMap<>();
  private final JCodeModel jCodeModel = new JCodeModel();
  private final JPackage jPackage;

  public JavaCodeGenerator(String packageName) {
    jPackage = jCodeModel._package(packageName);
  }

  JavaBuilder get(Schema schema) throws CodeGenerationException {
    if (schema == null) {
      throw new CodeGenerationException("Missing schema");
    }
    URI uri = schema.getUri();
    if (builtClasses.containsKey(uri)) {
      return builtClasses.get(uri);
    }

    return new JavaBuilder(this, schema);
  }

  public void register(URI uri, JavaBuilder javaBuilder) {
    builtClasses.put(uri, javaBuilder);
  }

  public void output(Path path) throws IOException {
    jCodeModel.build(path.toFile());
  }

  public JCodeModel getJCodeModel() {
    return jCodeModel;
  }

  public JPackage getJPackage() {
    return jPackage;
  }

  @Override
  public void build(Schema schema) throws CodeGenerationException {
    get(schema);
  }
}
