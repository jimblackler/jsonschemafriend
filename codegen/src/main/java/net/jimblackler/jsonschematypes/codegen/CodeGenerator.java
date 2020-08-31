package net.jimblackler.jsonschematypes.codegen;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.jimblackler.jsonschemafriend.DocumentUtils;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class CodeGenerator {
  private final JPackage jPackage;
  private final JCodeModel jCodeModel = new JCodeModel();
  private final Map<URI, Builder> builtClasses = new HashMap<>();

  public CodeGenerator(Path outPath, String packageName, URL resource1) throws IOException {
    jPackage = jCodeModel._package(packageName);

    SchemaStore schemaStore = new SchemaStore();
    URI defaultMetaSchema = URI.create("http://json-schema.org/draft-07/schema#");

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
      } catch (IOException | GenerationException e) {
        throw new IllegalStateException(e);
      }
    }
    jCodeModel.build(outPath.toFile());

    Files.walk(outPath).filter(Files::isRegularFile).forEach(path -> {
      try {
        String code;
        try (InputStream inputStream = path.toUri().toURL().openStream()) {
          code = DocumentUtils.streamToString(inputStream);
        }
        String formattedSource = new Formatter().formatSource(code);
        try (PrintWriter out = new PrintWriter(path.toFile(), "UTF-8")) {
          out.println(formattedSource);
        }
      } catch (FormatterException | IOException e) {
        // Ignored by design.
      }
    });
  }

  Builder getBuilder(Schema schema1) {
    URI uri = schema1.getUri();
    if (builtClasses.containsKey(uri)) {
      return builtClasses.get(uri);
    }

    if (!schema1.isObjectSchema()) {
      throw new IllegalStateException("Not sure what to do with these yet");
    }

    return new Builder(this, schema1);
  }

  public JCodeModel getJCodeModel() {
    return jCodeModel;
  }

  public void register(URI uri, Builder builder) {
    builtClasses.put(uri, builder);
  }

  public JPackage getJPackage() {
    return jPackage;
  }
}
