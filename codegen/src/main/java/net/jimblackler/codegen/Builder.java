package net.jimblackler.codegen;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
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
  private final String _name;
  private final JType dataType;

  public Builder(CodeGenerator codeGenerator, Schema schema1) {
    this.codeGenerator = codeGenerator;
    ObjectSchema schema = schema1.asObjectSchema();
    codeGenerator.register(schema.getUri(), this);

    JPackage jPackage = codeGenerator.getJPackage();
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();

    Set<String> types = schema.getTypes();

    if (types.size() == 1) {
      switch (types.iterator().next()) {
        case "array":
          dataType = jCodeModel.ref(JSONArray.class);
          break;
        case "boolean":
          dataType = jCodeModel.BOOLEAN;
          break;
        case "integer":
          dataType = jCodeModel.INT;
          break;
        case "null":
          dataType = jCodeModel.NULL;
          break;
        case "number":
          dataType = jCodeModel.ref(Number.class);
          break;
        case "object":
          dataType = jCodeModel.ref(JSONObject.class);
          break;
        case "string":
          dataType = jCodeModel.ref(String.class);
          break;
        default:
          throw new IllegalStateException();
      }
    } else {
      dataType = jCodeModel.ref(Object.class);
    }

    if (!(dataType.equals(jCodeModel.ref(Object.class))
            || dataType.equals(jCodeModel.ref(JSONArray.class))
            || dataType.equals(jCodeModel.ref(JSONObject.class)))) {
      jDefinedClass = null;
      _name = null;
      return;
    }

    Builder parent = codeGenerator.parent(schema.getUri());
    JClassContainer classParent;
    if (parent == null) {
      classParent = jPackage;
    } else {
      classParent = parent.getDefinedClass();
    }
    String name = nameForSchema(schema);
    JDefinedClass _class;
    while (true) {
      try {
        _class = classParent._class(parent == null ? JMod.PUBLIC : JMod.STATIC | JMod.PUBLIC, name);
        break;
      } catch (JClassAlreadyExistsException e) {
        name = varyName(name);
      }
    }
    jDefinedClass = _class;
    _name = name;
    StringBuilder docs = new StringBuilder();
    docs.append("Created from ").append(schema.getUri()).append(System.lineSeparator());
    docs.append("Explicit types ").append(schema.getExplicitTypes()).append(System.lineSeparator());
    docs.append("Inferred types ").append(schema.getInferredTypes()).append(System.lineSeparator());
    docs.append(schema.getSchemaJson().toString(2));

    jDefinedClass.javadoc().add(docs.toString());

    String dataObjectName = lowerCaseFirst(dataType.name());
    JFieldVar dataField = jDefinedClass.field(JMod.PUBLIC | JMod.FINAL, dataType, dataObjectName);

    /* Constructor */
    JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);

    JVar objectParam = constructor.param(dataType, dataObjectName);
    constructor.body().assign(JExpr._this().ref(dataField), objectParam);

    /* Getter */
    JMethod getter = jDefinedClass.method(JMod.PUBLIC, dataType, "get" + dataType.name());
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

  private static String lowerCaseFirst(String in) {
    in = in.replace("JSON", "Json");
    return Character.toLowerCase(in.charAt(0)) + in.substring(1);
  }

  private static JExpression castIfNeeded(JCodeModel jCodeModel, JClass _class, JFieldVar field) {
    if (field.type().equals(_class)) {
      return field;
    }
    return JExpr.cast(_class, field);
  }

  private static String getGet(JCodeModel jCodeModel, JType dataType) {
    if (dataType.equals(jCodeModel.ref(JSONObject.class))) {
      return "getJSONObject";
    }
    if (dataType.equals(jCodeModel.ref(JSONArray.class))) {
      return "getJSONArray";
    }
    if (dataType.equals(jCodeModel.BOOLEAN)) {
      return "getBoolean";
    }
    if (dataType.equals(jCodeModel.ref(String.class))) {
      return "getString";
    }
    if (dataType.equals(jCodeModel.INT)) {
      return "getInt";
    }
    if (dataType.equals(jCodeModel.ref(Number.class))) {
      return "getNumber";
    }

    return "get";
  }

  private static String varyName(String name) {
    for (int idx = 0; idx < name.length(); idx++) {
      try {
        int i = Integer.parseInt(name.substring(idx));
        return name.substring(0, idx) + (i + 1);
      } catch (NumberFormatException e) {
      }
    }
    return name + "2";
  }

  private JDefinedClass getDefinedClass() {
    return jDefinedClass;
  }

  private void writePropertyGetters(boolean requiredProperty, String propertyName,
      JDefinedClass holderClass, JFieldVar dataField) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    JExpression asJsonObject =
        castIfNeeded(jCodeModel, jCodeModel.ref(JSONObject.class), dataField);
    String get = getGet(jCodeModel, dataType);
    JType returnType;
    if (jDefinedClass == null) {
      returnType = dataType;
    } else {
      returnType = jDefinedClass;
    }
    JMethod getter =
        holderClass.method(JMod.PUBLIC, returnType, "get" + capitalizeFirst(propertyName));
    JInvocation getObject = JExpr.invoke(asJsonObject, get).arg(propertyName);
    if (jDefinedClass == null) {
      getter.body()._return(getObject);
    } else {
      getter.body()._return(JExpr._new(jDefinedClass).arg(getObject));
    }

    if (!requiredProperty) {
      JMethod has = holderClass.method(
          JMod.PUBLIC, jCodeModel.BOOLEAN, "has" + capitalizeFirst(propertyName));
      has.body()._return(JExpr.invoke(asJsonObject, "has").arg(propertyName));
    }
  }

  private void writeItemGetters(JDefinedClass holderClass, JFieldVar dataField) {
    if (jDefinedClass == null) {
    } else {
      JCodeModel jCodeModel = codeGenerator.getJCodeModel();
      JMethod getter =
          holderClass.method(JMod.PUBLIC, jDefinedClass, "get" + capitalizeFirst(_name));
      JVar indexParam = getter.param(jCodeModel.INT, "index");
      String get = getGet(jCodeModel, dataType);
      JExpression asJsonArray =
          castIfNeeded(jCodeModel, jCodeModel.ref(JSONArray.class), dataField);
      getter.body()._return(
          JExpr._new(jDefinedClass).arg(JExpr.invoke(asJsonArray, get).arg(indexParam)));
    }
  }
}
