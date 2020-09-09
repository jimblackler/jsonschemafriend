package net.jimblackler.jsonschematypes.codegen;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.MissingPathException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class CodeGenerator {
  private final JCodeModel jCodeModel = new JCodeModel();
  private final Map<URI, JavaBuilder> builtClasses = new HashMap<>();
  private final JPackage jPackage;
  private final SchemaStore schemaStore = new SchemaStore();
  private final URI defaultMetaSchema = URI.create("http://json-schema.org/draft-07/schema#");

  public CodeGenerator(String packageName) {
    jPackage = jCodeModel._package(packageName);
  }

  public void build(Path outPath, URL resource1) throws IOException {
    try (InputStream stream = resource1.openStream()) {
      try (BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (!resource.endsWith(".json")) {
            continue;
          }
          URI uri =
              URI.create(resource1 + (resource1.toString().endsWith("/") ? "" : "/") + resource);
          Schema schema = schemaStore.loadSchema(uri, defaultMetaSchema);
          getBuilder(schema);
        }
      } catch (IOException | GenerationException | MissingPathException e) {
        throw new IllegalStateException(e);
      }

      jCodeModel.build(outPath.toFile());
    }
  }

  public void build(Path outPath, URI uri)
      throws GenerationException, IOException, MissingPathException {
    getBuilder(schemaStore.loadSchema(uri, defaultMetaSchema));
    jCodeModel.build(outPath.toFile());
  }

  JavaBuilder getBuilder(Schema schema1) {
    URI uri = schema1.getUri();
    if (builtClasses.containsKey(uri)) {
      return builtClasses.get(uri);
    }

    return new JavaBuilder(this, schema1);
  }

  public JCodeModel getJCodeModel() {
    return jCodeModel;
  }

  public void register(URI uri, JavaBuilder javaBuilder) {
    builtClasses.put(uri, javaBuilder);
  }

  public JPackage getJPackage() {
    return jPackage;
  }
}
