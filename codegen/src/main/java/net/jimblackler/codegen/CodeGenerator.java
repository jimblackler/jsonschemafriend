package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Schema;
import net.jimblackler.jsonschematypes.SchemaStore;
import org.json.JSONArray;
import org.json.JSONObject;

public class CodeGenerator {
  private static final Logger LOG = Logger.getLogger(CodeGenerator.class.getName());

  private final JPackage jPackage;
  private final JCodeModel jCodeModel;
  private final Map<URI, JDefinedClass> builtClasses = new HashMap<>();
  private final Collection<String> usedNames = new HashSet<>();

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
            URI uri =
                URI.create(resource1 + (resource1.toString().endsWith("/") ? "" : "/") + resource);
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
    name = makeUnique(name);

    try {
      JDefinedClass _class = jPackage._class(name);
      builtClasses.put(schema.getUri(), _class);
      _class.javadoc().add("Created from " + schema.getUri());

      JFieldVar object = _class.field(JMod.PRIVATE | JMod.FINAL, Object.class, "object");

      JMethod constructor = _class.constructor(JMod.PUBLIC);
      JVar param = constructor.param(Object.class, "object");
      constructor.body().assign(JExpr._this().ref(object), param);

      {
        JMethod getter = _class.method(JMod.PUBLIC, Object.class, "getObject");
        getter.body()._return(object);
      }

      {
        JMethod getter = _class.method(JMod.PUBLIC, JSONObject.class, "getJSONObject");
        getter.body()._return(JExpr.cast(jCodeModel.ref(JSONObject.class), object));
      }

      {
        JMethod getter = _class.method(JMod.PUBLIC, JSONArray.class, "getJSONArray");
        getter.body()._return(JExpr.cast(jCodeModel.ref(JSONArray.class), object));
      }

      Map<String, Schema> properties = schema.getProperties();
      for (Map.Entry<String, Schema> entry : properties.entrySet()) {
        String propertyName = entry.getKey();
        Schema propertySchema = entry.getValue();
        if (propertySchema == null) {
          LOG.warning(schema.getUri() + ": No valid property " + propertyName);
        } else {
          JDefinedClass propertyJClass = getClass(propertySchema);
          JMethod propertyGetter =
              _class.method(JMod.PUBLIC, propertyJClass, "get" + capitalizeFirst(propertyName));
          propertyGetter.body()._return(
              JExpr._new(propertyJClass)
                  .arg(JExpr.invoke(JExpr.cast(jCodeModel.ref(JSONObject.class), object), "get")
                      .arg(propertyName)));
        }
      }

      return _class;
    } catch (JClassAlreadyExistsException e) {
      throw new IllegalStateException(e);
    }
  }

  private String makeUnique(String name) {
    if (usedNames.add(name)) {
      return name;
    }

    for (int idx = 0; idx < name.length(); idx++) {
      try {
        int i = Integer.parseInt(name.substring(idx));
        return makeUnique(name.substring(0, idx) + (i + 1));
      } catch (NumberFormatException e) {
      }
    }
    return makeUnique(name + "2");
  }

  private static String nameForSchema(Schema schema) {
    String[] split = schema.getUri().toString().split("/");
    String lastPart = split[split.length - 1];
    String namePart = lastPart.split("\\.", 2)[0];
    return capitalizeFirst(namePart);
  }

  private static String capitalizeFirst(String in) {
    return Character.toUpperCase(in.charAt(0)) + in.substring(1);
  }
}
