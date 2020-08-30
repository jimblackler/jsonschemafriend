package net.jimblackler.codegen;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import java.util.Map;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class Builder {
  private final CodeGenerator codeGenerator;
  private final JDefinedClass jDefinedClass;
  private final ObjectSchema schema;
  private final String _name;

  public Builder(CodeGenerator codeGenerator, Schema schema1) {
    this.codeGenerator = codeGenerator;
    schema = schema1.asObjectSchema();
    codeGenerator.register(schema.getUri(), this);

    _name = codeGenerator.makeUnique(nameForSchema(schema));

    try {
      JPackage jPackage = codeGenerator.getJPackage();
      JCodeModel jCodeModel = codeGenerator.getJCodeModel();
      jDefinedClass = jPackage._class(_name);

      StringBuilder docs = new StringBuilder();
      docs.append("Created from ").append(schema.getUri()).append(System.lineSeparator());
      docs.append("Explicit types ")
          .append(schema.getExplicitTypes())
          .append(System.lineSeparator());
      docs.append("Inferred types ")
          .append(schema.getInferredTypes())
          .append(System.lineSeparator());
      docs.append(schema.getSchemaJson().toString(2));

      jDefinedClass.javadoc().add(docs.toString());

      /* Plain Object field */
      JFieldVar objectField =
          jDefinedClass.field(JMod.PUBLIC | JMod.FINAL, jCodeModel.ref(Object.class), "object");

      JType _type = jCodeModel.ref(Object.class);

      /* Constructor */
      JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
      JVar objectParam = constructor.param(_type, "object");
      constructor.body().assign(JExpr._this().ref(objectField), objectParam);

      /* Getter */
      JMethod getter = jDefinedClass.method(JMod.PUBLIC, _type, "getObject");
      getter.body()._return(objectField);

      for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
        Builder builder = codeGenerator.getBuilder(entry.getValue());
        String propertyName = entry.getKey();
        builder.writePropertyGetters(schema.getRequiredProperties().contains(propertyName),
            propertyName, jDefinedClass, objectField);
      }

      for (Schema itemsSchema : schema.getItems()) {
        Builder builder = codeGenerator.getBuilder(itemsSchema);
        builder.writeItemGetters(jDefinedClass, objectField);
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

  private void writePropertyGetters(
      boolean requiredProperty, String propertyName, JDefinedClass holderClass, JFieldVar object) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    JMethod getter =
        holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(propertyName));
    getter.body()._return(
        JExpr._new(jDefinedClass)
            .arg(JExpr.invoke(JExpr.cast(jCodeModel.ref(JSONObject.class), object), "get")
                     .arg(propertyName)));

    if (!requiredProperty) {
      JMethod has = holderClass.method(
          JMod.PUBLIC, jCodeModel.BOOLEAN, "has" + capitalizeFirst(propertyName));
      has.body()._return(JExpr.invoke(JExpr.cast(jCodeModel.ref(JSONObject.class), object), "has")
                             .arg(propertyName));
    }
  }

  private void writeItemGetters(JDefinedClass holderClass, JFieldVar object) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();

    JMethod getter = holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(_name));
    JVar indexParam = getter.param(jCodeModel.INT, "index");
    getter.body()._return(
        JExpr._new(jDefinedClass)
            .arg(JExpr.invoke(JExpr.cast(jCodeModel.ref(JSONArray.class), object), "get")
                     .arg(indexParam)));
  }
}
