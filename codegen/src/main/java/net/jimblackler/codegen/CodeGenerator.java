package net.jimblackler.codegen;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

public class CodeGenerator {
  private final JPackage jPackage;
  private final JCodeModel jCodeModel = new JCodeModel();
  private final Map<URI, Builder> builtClasses = new HashMap<>();
  private final Collection<String> usedNames = new HashSet<>();

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

  public Builder parent(URI uri) {
    while (true) {
      String path = uri.getRawFragment();
      if (path == null) {
        return null;
      }
      int i = path.lastIndexOf("/");
      if (i <= 0) {
        return null;
      }
      try {
        uri = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), path.substring(0, i));
      } catch (URISyntaxException e) {
        throw new IllegalStateException(e);
      }
      if (builtClasses.containsKey(uri)) {
        return builtClasses.get(uri);
      }
    }
  }

  public JPackage getJPackage() {
    return jPackage;
  }
}
