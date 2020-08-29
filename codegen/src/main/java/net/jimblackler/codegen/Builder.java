package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import java.util.Map;
import java.util.logging.Logger;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class Builder {
  private static final Logger LOG = Logger.getLogger(Builder.class.getName());
  private final CodeGenerator codeGenerator;
  private final JDefinedClass jDefinedClass;

  public Builder(CodeGenerator codeGenerator, Schema schema1) {
    this.codeGenerator = codeGenerator;
    ObjectSchema schema = schema1.asObjectSchema();

    String name = nameForSchema(schema);
    name = codeGenerator.makeUnique(name);

    try {
      JCodeModel jCodeModel = codeGenerator.getJCodeModel();
      jDefinedClass = jCodeModel._class(name);
      codeGenerator.register(schema.getUri(), this);
      jDefinedClass.javadoc().add("Created from " + schema.getUri());

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

    JMethod propertyGetter =
        holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(propertyName));
    propertyGetter.body()._return(
        JExpr._new(jDefinedClass)
            .arg(JExpr.invoke(JExpr.cast(jCodeModel.ref(JSONObject.class), holderObject), "get")
                     .arg(propertyName)));
  }
}
