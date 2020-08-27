package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Schema;
import net.jimblackler.jsonschematypes.SchemaStore;
import org.json.JSONObject;

public class CodeGenerator {
  static void generate(Schema schema, Path outPath, String _package) throws IOException {
    JCodeModel codeModel = new JCodeModel();
    JPackage jp = codeModel._package(_package);

    String name = nameForSchema(schema);

    try {
      JDefinedClass _class = jp._class(name);
      _class.javadoc().add(schema.getUri().toString());

      _class.constructor(JMod.PUBLIC);
      _class.constructor(JMod.PUBLIC).param(JSONObject.class, "object");

      _class.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(1L));
      JFieldVar quantity = _class.field(JMod.PRIVATE, Integer.class, "quantity");

      JMethod getter = _class.method(JMod.PUBLIC, quantity.type(), "getQuantity");
      getter.body()._return(quantity);
      getter.javadoc().addReturn().add(quantity.name());

      JMethod setter = _class.method(JMod.PUBLIC, codeModel.VOID, "setQuantity");
      setter.param(quantity.type(), quantity.name());
      setter.body().assign(JExpr._this().ref(quantity.name()), JExpr.ref(quantity.name()));

    } catch (JClassAlreadyExistsException e) {
      throw new IllegalStateException(e);
    }

    // Generate the code
    codeModel.build(outPath.toFile());
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

  public static void outputTypes(Path outPath, String packageName, URL resource1)
      throws IOException {
    if (Files.exists(outPath)) {
      // Empty the directory if it already exists.
      try (Stream<Path> files = Files.walk(outPath)) {
        files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
      }
    }
    Files.createDirectories(outPath);

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
            generate(schema, outPath, packageName);
          }
        }
      } catch (IOException | GenerationException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
