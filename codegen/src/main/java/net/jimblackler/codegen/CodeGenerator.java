package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
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
import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Schema;
import net.jimblackler.jsonschematypes.SchemaStore;
import org.json.JSONObject;

public class CodeGenerator {
  private final JPackage jPackage;
  private final JCodeModel jCodeModel;
  private final Map<URI, JDefinedClass> builtClasses = new HashMap<>();

  public CodeGenerator(Path outPath, String packageName, URL resource1) throws IOException {
    jCodeModel = new JCodeModel();
    jPackage = jCodeModel._package(packageName);

    SchemaStore schemaStore = new SchemaStore();
    URI defaultMetaSchema = URI.create("http://json-schema.org/draft-07/schema#");

    try (InputStream stream = resource1.openStream()) {
      try (BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
        String resource;
        while ((resource = bufferedReader.readLine()) != null) {
          if (resource.endsWith(".json")) {
            URI uri = URI.create(resource1 + "/" + resource);
            Schema schema = schemaStore.validateAndGet(uri, defaultMetaSchema);
            getClass(schema);
          }
        }
      } catch (IOException | GenerationException e) {
        throw new IllegalStateException(e);
      }
    }
    jCodeModel.build(outPath.toFile());
  }

  private JDefinedClass getClass(Schema schema) {
    if (builtClasses.containsKey(schema.getUri())) {
      return builtClasses.get(schema.getUri());
    }

    String name = nameForSchema(schema);

    try {
      JDefinedClass _class = jPackage._class(name);
      builtClasses.put(schema.getUri(), _class);
      _class.javadoc().add("Created from " + schema.getUri());

      _class.constructor(JMod.PUBLIC).param(JSONObject.class, "object");

      JFieldVar jsonObject = _class.field(JMod.FINAL, JSONObject.class, "jsonObject");

      JMethod getter = _class.method(JMod.PUBLIC, JSONObject.class, "getJsonObject");
      getter.body()._return(jsonObject);

      return _class;
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalStateException(e);
    }
  }

  private static String nameForSchema(Schema schema) {
    String[] split = schema.getUri().toString().split("/");
    String lastPart = split[split.length - 1];
    String namePart = lastPart.split("\\.", 2)[0];
    return capitalizeFirst(namePart);
  }

  private static String capitalizeFirst(String in) {
    StringBuilder out = new StringBuilder(in.length());
    out.append(Character.toUpperCase(in.charAt(0)));
    out.append(in.substring(1).toLowerCase());
    return out.toString();
  }
}
