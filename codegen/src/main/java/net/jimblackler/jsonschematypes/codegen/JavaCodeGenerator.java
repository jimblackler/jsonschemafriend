package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschematypes.codegen.NameUtils.makeJavaLegalPackage;

import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JPackage;
import com.helger.jcodemodel.writer.AbstractCodeWriter;
import com.helger.jcodemodel.writer.FileCodeWriter;
import com.helger.jcodemodel.writer.JCMWriter;
import com.helger.jcodemodel.writer.OutputStreamCodeWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;

public class JavaCodeGenerator implements CodeGenerator {
  private final Map<URI, JavaBuilder> builtClasses = new HashMap<>();
  private final JCodeModel jCodeModel = new JCodeModel();
  private final JPackage jPackage;

  public JavaCodeGenerator(String packageName) {
    jPackage = jCodeModel._package(makeJavaLegalPackage(packageName));
  }

  JavaBuilder get(Schema schema) throws CodeGenerationException {
    if (schema == null) {
      return null;
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

  public void output(OutputStream stream) throws IOException {
    _output(new OutputStreamCodeWriter(
        stream, StandardCharsets.UTF_8, System.getProperty("line.separator")));
  }

  public void output(Path path) throws IOException {
    _output(new FileCodeWriter(
        path.toFile(), StandardCharsets.UTF_8, System.getProperty("line.separator")));
  }

  private void _output(AbstractCodeWriter writer) throws IOException {
    JCMWriter jcmWriter = new JCMWriter(jCodeModel);
    jcmWriter.setIndentString("\t");
    jcmWriter.build(writer, writer);
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
