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
  private final String _name;
  private final JType dataType;

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

      Set<String> types = schema.getTypes();

      /* Plain Object field */

      if (types.size() == 1 && types.contains("object")) {
        dataType = jCodeModel.ref(JSONObject.class);
      } else if (types.size() == 1 && types.contains("array")) {
        dataType = jCodeModel.ref(JSONArray.class);
      } else {
        dataType = jCodeModel.ref(Object.class);
      }

      JFieldVar dataField = jDefinedClass.field(JMod.PUBLIC | JMod.FINAL, dataType, "object");

      /* Constructor */
      JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);
      JVar objectParam = constructor.param(dataType, "object");
      constructor.body().assign(JExpr._this().ref(dataField), objectParam);

      /* Getter */
      JMethod getter = jDefinedClass.method(JMod.PUBLIC, dataType, "getObject");
      getter.body()._return(dataField);

      for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
        Builder builder = codeGenerator.getBuilder(entry.getValue());
        String propertyName = entry.getKey();
        builder.writePropertyGetters(schema.getRequiredProperties().contains(propertyName),
            propertyName, jDefinedClass, dataField);
      }

      for (Schema itemsSchema : schema.getItems()) {
        Builder builder = codeGenerator.getBuilder(itemsSchema);
        builder.writeItemGetters(jDefinedClass, dataField);
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

  private static JExpression castIfNeeded(JCodeModel jCodeModel, JClass _class, JFieldVar field) {
    if (field.type().equals(_class)) {
      return field;
    }
    return JExpr.cast(_class, field);
  }

  private static String getGet(JCodeModel jCodeModel, JType dataType) {
    String get;
    if (dataType.equals(jCodeModel.ref(JSONObject.class))) {
      get = "getJSONObject";
    } else if (dataType.equals(jCodeModel.ref(JSONArray.class))) {
      get = "getJSONArray";
    } else if (dataType.equals(jCodeModel.ref(Object.class))) {
      get = "get";
    } else {
      throw new IllegalStateException();
    }
    return get;
  }

  private void writePropertyGetters(boolean requiredProperty, String propertyName,
      JDefinedClass holderClass, JFieldVar dataField) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    JMethod getter =
        holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(propertyName));
    JExpression asJsonObject =
        castIfNeeded(jCodeModel, jCodeModel.ref(JSONObject.class), dataField);

    String get = getGet(jCodeModel, dataType);

    getter.body()._return(
        JExpr._new(jDefinedClass).arg(JExpr.invoke(asJsonObject, get).arg(propertyName)));
    if (!requiredProperty) {
      JMethod has = holderClass.method(
          JMod.PUBLIC, jCodeModel.BOOLEAN, "has" + capitalizeFirst(propertyName));
      has.body()._return(JExpr.invoke(asJsonObject, "has").arg(propertyName));
    }
  }

  private void writeItemGetters(JDefinedClass holderClass, JFieldVar dataField) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();

    JMethod getter = holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(_name));
    JVar indexParam = getter.param(jCodeModel.INT, "index");
    String get = getGet(jCodeModel, dataType);
    getter.body()._return(
        JExpr._new(jDefinedClass)
            .arg(JExpr
                     .invoke(
                         castIfNeeded(jCodeModel, jCodeModel.ref(JSONArray.class), dataField), get)
                     .arg(indexParam)));
  }
}
