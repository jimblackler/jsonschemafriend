package net.jimblackler.jsonschematypes.codegen;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JSwitch;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class JavaBuilder {
  private final CodeGenerator codeGenerator;
  private final JDefinedClass jDefinedClass;
  private final String _name;
  private final JType dataType;
  private final List<JEnumConstant> enumConstants = new ArrayList<>();
  private final Schema schema;

  public JavaBuilder(CodeGenerator codeGenerator, Schema schema) {
    this.codeGenerator = codeGenerator;
    this.schema = schema;

    codeGenerator.register(schema.getUri(), this);

    JPackage jPackage = codeGenerator.getJPackage();
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();

    Collection<String> types = schema.getTypes();

    if (types.size() == 1) {
      switch (types.iterator().next()) {
        case "array":
          dataType = jCodeModel.ref(JSONArray.class);
          break;
        case "boolean":
          dataType = jCodeModel.BOOLEAN;
          break;
        case "integer":
          // JSON Schema's definition of an integer is not the same as Java's.
          // Specifically, values over 2^32 are supported. We use a Java Long.
          dataType = jCodeModel.LONG;
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

    Schema parentSchema = schema.getParent();
    JClassContainer classParent =
        parentSchema == null ? jPackage : codeGenerator.build(parentSchema).getDefinedClass();

    String name = nameForSchema(schema);
    boolean isComplexObject =
        dataType.equals(jCodeModel.ref(JSONObject.class)) && !schema.getProperties().isEmpty();
    if (isComplexObject || dataType.equals(jCodeModel.ref(Object.class))
        || dataType.equals(jCodeModel.ref(JSONArray.class))) {
      JDefinedClass _class = DefinedClassMaker.makeClassForSchema(classParent, name,
          (name12)
              -> classParent._class(
                  parentSchema == null ? JMod.PUBLIC : JMod.STATIC | JMod.PUBLIC, name12));

      jDefinedClass = _class;
      _name = _class.name();

      StringBuilder docs = new StringBuilder();
      docs.append("Created from ").append(schema.getUri()).append(System.lineSeparator());
      docs.append("Explicit types ")
          .append(schema.getExplicitTypes())
          .append(System.lineSeparator());
      docs.append("Inferred types ")
          .append(schema.getInferredTypes())
          .append(System.lineSeparator());
      // docs.append("<pre>").append(schema.getSchemaJson().toString(2)).append("</pre>");

      jDefinedClass.javadoc().add(docs.toString());

      String name1 = dataType.name().replace("JSON", "Json");
      String dataObjectName = NameUtils.lowerCaseFirst(NameUtils.snakeToCamel(name1));
      JFieldVar dataField =
          jDefinedClass.field(JMod.PRIVATE | JMod.FINAL, dataType, dataObjectName);

      /* Constructor */
      JMethod constructor = jDefinedClass.constructor(JMod.PUBLIC);

      JVar objectParam = constructor.param(dataType, dataObjectName);
      constructor.body().assign(JExpr._this().ref(dataField), objectParam);

      /* Getter */
      JMethod getter = jDefinedClass.method(JMod.PUBLIC, dataType,
          (dataType.equals(jCodeModel.BOOLEAN) ? "is" : "get") + dataType.name());
      getter.body()._return(dataField);

      for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
        Schema propertySchema = entry.getValue();
        JavaBuilder javaBuilder = codeGenerator.build(propertySchema);
        String propertyName = entry.getKey();
        javaBuilder.writePropertyGetters(schema.getRequiredProperties().contains(propertyName),
            expressionFromObject(propertySchema.getDefault()), jDefinedClass, dataField,
            propertyName);
      }

      Collection<Schema> itemsTuple = schema.getItemsTuple();
      if (itemsTuple != null) {
        for (Schema itemsSchema : itemsTuple) {
          JavaBuilder javaBuilder = codeGenerator.build(itemsSchema);
          javaBuilder.writeItemGetters(
              jDefinedClass, expressionFromObject(itemsSchema.getDefault()), dataField);
        }
      }

      Schema _items = schema.getItems();
      if (_items != null) {
        JavaBuilder javaBuilder = codeGenerator.build(_items);
        javaBuilder.writeItemGetters(
            jDefinedClass, expressionFromObject(_items.getDefault()), dataField);
      }

      if (types.contains("array")) {
        JMethod sizeMethod = jDefinedClass.method(JMod.PUBLIC, jCodeModel.INT, "size");
        JExpression asJsonArray = castIfNeeded(jCodeModel.ref(JSONArray.class), dataField);
        sizeMethod.body()._return(JExpr.invoke(asJsonArray, "length"));
      }
    } else if (schema.getEnums() != null && dataType.equals(jCodeModel.ref(String.class))) {
      List<Object> enums = schema.getEnums();
      JDefinedClass _enum =
          DefinedClassMaker.makeClassForSchema(classParent, name, classParent::_enum);

      StringBuilder docs = new StringBuilder();
      docs.append("Created from ").append(schema.getUri()).append(System.lineSeparator());
      _enum.javadoc().add(docs.toString());

      _name = _enum.name();

      for (Object value : enums) {
        JEnumConstant enumConstant =
            _enum.enumConstant(NameUtils.camelToSnake((String) value).toUpperCase());
        enumConstants.add(enumConstant);
      }

      jDefinedClass = _enum;
    } else {
      jDefinedClass = null;
      _name = name;
    }
  }

  private static JExpression expressionFromObject(Object object) {
    if (object instanceof Integer) {
      return JExpr.lit((Integer) object);
    }

    if (object instanceof Long) {
      return JExpr.lit((Long) object);
    }

    if (object instanceof Float) {
      return JExpr.lit((Float) object);
    }

    if (object instanceof Boolean) {
      return JExpr.lit((Boolean) object);
    }

    if (object instanceof Double) {
      return JExpr.lit((Double) object);
    }

    if (object instanceof Character) {
      return JExpr.lit((Character) object);
    }

    if (object instanceof String) {
      return JExpr.lit((String) object);
    }

    return null;
  }

  private static String nameForSchema(Schema schema) {
    String[] split = schema.getUri().toString().split("/");
    String lastPart = split[split.length - 1];
    String namePart = lastPart.split("\\.", 2)[0];

    String converted = NameUtils.snakeToCamel(namePart);
    if (!Character.isJavaIdentifierStart(converted.charAt(0))) {
      converted = "_" + converted;
    }

    StringBuilder builder = new StringBuilder();
    for (int idx = 0; idx != converted.length(); idx++) {
      char chr = converted.charAt(idx);
      if (Character.isJavaIdentifierPart(chr)) {
        builder.append(chr);
      }
    }

    return builder.toString();
  }

  private static JExpression castIfNeeded(JClass _class, JFieldVar field) {
    return field.type().equals(_class) ? field : JExpr.cast(_class, field);
  }

  private static String getOptOrGet(boolean get, JType dataType, JCodeModel jCodeModel) {
    String kind = get ? "get" : "opt";
    if (dataType.equals(jCodeModel.ref(JSONObject.class))) {
      return kind + "JSONObject";
    }
    if (dataType.equals(jCodeModel.ref(JSONArray.class))) {
      return kind + "JSONArray";
    }
    if (dataType.equals(jCodeModel.BOOLEAN)) {
      return kind + "Boolean";
    }
    if (dataType.equals(jCodeModel.ref(String.class))) {
      return kind + "String";
    }
    if (dataType.equals(jCodeModel.LONG)) {
      return kind + "Long";
    }
    if (dataType.equals(jCodeModel.INT)) {
      return kind + "Int";
    }
    if (dataType.equals(jCodeModel.ref(Number.class))) {
      return kind + "Number";
    }

    return "get";
  }

  JDefinedClass getDefinedClass() {
    return jDefinedClass;
  }

  private void writePropertyGetters(boolean requiredProperty, JExpression defaultValue,
      JDefinedClass holderClass, JFieldVar dataField, String propertyName) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    JExpression asJsonObject = castIfNeeded(jCodeModel.ref(JSONObject.class), dataField);
    String nameForGetters = NameUtils.snakeToCamel(propertyName);
    JType returnType;
    if (jDefinedClass == null) {
      returnType = dataType;
    } else {
      returnType = jDefinedClass;
    }
    JMethod getter = holderClass.method(JMod.PUBLIC, returnType,
        (returnType.equals(jCodeModel.BOOLEAN) ? "is" : "get") + nameForGetters);
    boolean isGet = defaultValue == null;
    JInvocation getObject =
        JExpr.invoke(asJsonObject, getOptOrGet(isGet, dataType, jCodeModel)).arg(propertyName);
    if (defaultValue != null && !defaultValue.equals(JExpr.lit(false))) {
      getObject.arg(defaultValue);
    }
    if (jDefinedClass == null) {
      getter.body()._return(getObject);
    } else if (enumConstants.isEmpty()) {
      getter.body()._return(JExpr._new(jDefinedClass).arg(getObject));
    } else {
      JVar value = getter.body().decl(jCodeModel.ref(String.class), "value").init(getObject);
      List<Object> enums = schema.getEnums();
      JSwitch jSwitch = getter.body()._switch(value);
      for (int idx = 0; idx != enums.size(); idx++) {
        String enumString = (String) enums.get(idx);
        jSwitch._case(JExpr.lit(enumString)).body()._return(enumConstants.get(idx));
      }

      getter.body()._throw(JExpr._new(jCodeModel.ref(IllegalStateException.class))
                               .arg(JExpr.lit("Unexpected enum ").plus(value)));
    }

    if (!requiredProperty && isGet) {
      JMethod has = holderClass.method(JMod.PUBLIC, jCodeModel.BOOLEAN, "has" + nameForGetters);
      has.body()._return(JExpr.invoke(asJsonObject, "has").arg(propertyName));
    }
  }

  private void writeItemGetters(
      JDefinedClass holderClass, JExpression defaultValue, JFieldVar dataField) {
    JCodeModel jCodeModel = codeGenerator.getJCodeModel();
    JExpression asJsonArray = castIfNeeded(jCodeModel.ref(JSONArray.class), dataField);
    String nameForGetters = _name;
    JType returnType;
    if (jDefinedClass == null) {
      returnType = dataType;
    } else {
      returnType = jDefinedClass;
    }
    JMethod getter = holderClass.method(JMod.PUBLIC, returnType,
        (returnType.equals(jCodeModel.BOOLEAN) ? "is" : "get") + nameForGetters);
    JVar indexParam = getter.param(jCodeModel.INT, "index");
    boolean isGet = defaultValue == null;
    JInvocation getObject =
        JExpr.invoke(asJsonArray, getOptOrGet(isGet, dataType, jCodeModel)).arg(indexParam);
    if (defaultValue != null && !defaultValue.equals(JExpr.lit(false))) {
      getObject.arg(defaultValue);
    }
    if (jDefinedClass == null) {
      getter.body()._return(getObject);
    } else {
      getter.body()._return(JExpr._new(jDefinedClass).arg(getObject));
    }
  }
}
