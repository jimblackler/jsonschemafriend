package net.jimblackler.codegen;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.jimblackler.jsonschemafriend.ObjectSchema;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class Builder {
  private final CodeGenerator codeGenerator;
  private final JDefinedClass jDefinedClass;
  private final ObjectSchema schema;

  public Builder(CodeGenerator codeGenerator, Schema schema1) {
    this.codeGenerator = codeGenerator;
    schema = schema1.asObjectSchema();
    codeGenerator.register(schema.getUri(), this);

    String name = nameForSchema(schema);
    name = codeGenerator.makeUnique(name);

    try {
      JPackage jPackage = codeGenerator.getJPackage();
      JCodeModel jCodeModel = codeGenerator.getJCodeModel();
      jDefinedClass = jPackage._class(name);

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

      JFieldVar objectField =
          jDefinedClass.field(JMod.PUBLIC | JMod.FINAL, jCodeModel.ref(Object.class), "object");

      JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
      JVar objectParam = constructor.param(jCodeModel.ref(Object.class), "object");
      constructor.body().assign(JExpr._this().ref(objectField), objectParam);

      JMethod getter = jDefinedClass.method(
          JMod.PUBLIC, jCodeModel.ref(Object.class), "get" + capitalizeFirst(name));
      getter.body()._return(objectField);

      for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
        Builder builder = codeGenerator.getClass(entry.getValue());
        builder.writeGetters(entry.getKey(), jDefinedClass, objectField);
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

  private void writeGetters(String propertyName, JDefinedClass holderClass, JFieldVar object) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    Set<String> types = schema.getTypes();

    JMethod propertyGetter =
        holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(propertyName));
    propertyGetter.body()._return(JExpr._new(jDefinedClass).arg(object));
  }

  private void makeGetters(JClass type, String name, JDefinedClass holderClass, String propertyName,
      JExpression jsonArray, JExpression jsonObject) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    if (jsonObject != null) {
      JMethod propertyGetter =
          holderClass.method(JMod.PUBLIC, type, "get" + capitalizeFirst(propertyName));
      propertyGetter.body()._return(JExpr.invoke(jsonObject, "get" + name).arg(propertyName));
    }
    if (jsonArray != null) {
      JMethod propertyGetter =
          holderClass.method(JMod.PUBLIC, type, "get" + capitalizeFirst(propertyName));
      propertyGetter.varParam(jCodeModel.INT, "index");
      propertyGetter.body()._return(JExpr.invoke(jsonArray, "get" + name).arg(propertyName));
    }
  }
}
