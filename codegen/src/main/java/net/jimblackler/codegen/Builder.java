package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class Builder {
  private static final Logger LOG = Logger.getLogger(Builder.class.getName());
  private final CodeGenerator codeGenerator;
  private final JDefinedClass jDefinedClass;
  private final ObjectSchema schema;

  public Builder(CodeGenerator codeGenerator, Schema schema1) {
    this.codeGenerator = codeGenerator;
    schema = schema1.asObjectSchema();

    Set<String> types = schema.getExplicitTypes();
    if (types != null && types.size() == 1) {
      if (types.contains("string")) {
        jDefinedClass = null;
        return;
      }
    }

    String name = nameForSchema(schema);
    name = codeGenerator.makeUnique(name);

    try {
      JPackage jPackage = codeGenerator.getJPackage();
      JCodeModel jCodeModel = codeGenerator.getJCodeModel();
      jDefinedClass = jPackage._class(name);
      codeGenerator.register(schema.getUri(), this);
      jDefinedClass.javadoc().add("Created from " + schema.getUri() + System.lineSeparator()
          + schema.getSchemaJson().toString(2));

      JFieldVar object = jDefinedClass.field(JMod.PRIVATE | JMod.FINAL, Object.class, "object");

      JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
      JVar param = constructor.param(Object.class, "object");
      constructor.body().assign(JExpr._this().ref(object), param);

      {
        JMethod getter = jDefinedClass.method(JMod.PUBLIC, Object.class, "getObject");
        getter.body()._return(object);
      }

      {
        JMethod getter = jDefinedClass.method(JMod.PUBLIC, JSONObject.class, "getJSONObject");
        getter.body()._return(JExpr.cast(jCodeModel.ref(JSONObject.class), object));
      }

      {
        JMethod getter = jDefinedClass.method(JMod.PUBLIC, JSONArray.class, "getJSONArray");
        getter.body()._return(JExpr.cast(jCodeModel.ref(JSONArray.class), object));
      }

      Map<String, Schema> properties = schema.getProperties();
      for (Map.Entry<String, Schema> entry : properties.entrySet()) {
        String propertyName = entry.getKey();
        Schema propertySchema = entry.getValue();
        if (propertySchema == null) {
          LOG.warning(schema.getUri() + ": No valid property " + propertyName);
        } else {
          Builder builder = codeGenerator.getClass(propertySchema);
          builder.writeGetters(propertyName, jDefinedClass, object);
        }
      }

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
    return Character.toUpperCase(in.charAt(0)) + in.substring(1);
  }

  private void writeGetters(
      String propertyName, JDefinedClass holderClass, JFieldVar holderObject) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();

    JExpression holderObjectAsJsonObject =
        JExpr.cast(jCodeModel.ref(JSONObject.class), holderObject);
    Set<String> types = schema.getExplicitTypes();
    if (types != null && types.size() == 1) {
      //      if (types.contains("array")) { } else
      //      if (types.contains("boolean")) { } else
      //      if (types.contains("integer")) { } else
      //      if (types.contains("null")) { } else
      //      if (types.contains("number")) { } else
      //      if (types.contains("object")) { } else
      if (types.contains("string")) {
        JMethod propertyGetter =
            holderClass.method(JMod.PUBLIC, String.class, "get" + capitalizeFirst(propertyName));
        propertyGetter.body()._return(
            JExpr.invoke(holderObjectAsJsonObject, "getString").arg(propertyName));
        return;
      }
    }

    {
      JMethod propertyGetter =
          holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(propertyName));
      propertyGetter.body()._return(
          JExpr._new(jDefinedClass)
              .arg(JExpr.invoke(holderObjectAsJsonObject, "get").arg(propertyName)));
    }
  }
}
