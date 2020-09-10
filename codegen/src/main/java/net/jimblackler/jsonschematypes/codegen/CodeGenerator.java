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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
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

  public void build(Path outPath, URL url) throws CodeGenerationException {
    try {
      List<Schema> schemas = getSchemas(defaultMetaSchema, schemaStore, url);
      for (Schema schema : schemas) {
        getBuilder(schema);
      }
      build(outPath);
    } catch (SchemaException | IOException e) {
      throw new CodeGenerationException(e);
    }
  }

  public void build(Path outPath, URI uri) throws CodeGenerationException {
    try {
      getBuilder(schemaStore.loadSchema(uri, defaultMetaSchema));
      build(outPath);
    } catch (SchemaException | IOException e) {
      throw new CodeGenerationException(e);
    }
  }

  private void build(Path outPath) throws IOException {
    jCodeModel.build(outPath.toFile());
  }

  private static List<Schema> getSchemas(URI defaultMetaSchema, SchemaStore schemaStore,
      URL resource1) throws IOException, SchemaException {
    List<Schema> schemas = new ArrayList<>();
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
          schemas.add(schemaStore.loadSchema(uri, defaultMetaSchema));
        }
      }
    }
    return schemas;
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
