package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschematypes.codegen.JavaDefinedClassMaker.makeClassForSchema;
import static net.jimblackler.jsonschematypes.codegen.NameUtils.camelToSnake;
import static net.jimblackler.jsonschematypes.codegen.NameUtils.makeJavaLegal;
import static net.jimblackler.jsonschematypes.codegen.NameUtils.nameForSchema;

import com.helger.jcodemodel.AbstractJClass;
import com.helger.jcodemodel.AbstractJType;
import com.helger.jcodemodel.IJClassContainer;
import com.helger.jcodemodel.IJExpression;
import com.helger.jcodemodel.JBlock;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JEnumConstant;
import com.helger.jcodemodel.JExpr;
import com.helger.jcodemodel.JFieldVar;
import com.helger.jcodemodel.JInvocation;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;
import com.helger.jcodemodel.JPackage;
import com.helger.jcodemodel.JSwitch;
import com.helger.jcodemodel.JVar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.jimblackler.jsonschemafriend.CombinedSchema;
import net.jimblackler.jsonschemafriend.Schema;
import org.json.JSONArray;
import org.json.JSONObject;

public class JavaBuilder {
  private final JDefinedClass jDefinedClass;
  private final String _name;
  private final AbstractJType dataType;
  private final List<JEnumConstant> enumConstants = new ArrayList<>();
  private final Schema schema;

  public JavaBuilder(JavaCodeGenerator javaCodeGenerator, Schema schema)
      throws CodeGenerationException {
    this.schema = schema;
    CombinedSchema combinedSchema = new CombinedSchema(schema);
    JCodeModel jCodeModel = javaCodeGenerator.getJCodeModel();
    JPackage jPackage = javaCodeGenerator.getJPackage();
    javaCodeGenerator.register(schema.getUri(), this);

    Collection<String> types = combinedSchema.getInferredTypes();

    if (types.size() == 1) {
      switch (types.iterator().next()) {
        case "array":
          dataType = jCodeModel.ref(JSONArray.class);
          break;
        case "boolean":
          dataType = jCodeModel.BOOLEAN;
          break;
        case "integer":
          Number minimumObject = schema.getMinimum();
          long minimum = minimumObject == null ? Long.MIN_VALUE : minimumObject.longValue();
          Number maximumObject = schema.getMinimum();
          long maximum = maximumObject == null ? Long.MAX_VALUE : maximumObject.longValue();
          if (minimum >= Integer.MIN_VALUE && maximum <= Integer.MAX_VALUE) {
            dataType = jCodeModel.INT;
          } else {
            // JSON Schema's definition of an integer is not the same as Java's.
            // Specifically, values over 2^32 are supported. We use a Java Long.
            dataType = jCodeModel.LONG;
          }
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
    final IJClassContainer<JDefinedClass> classParent;
    if (parentSchema == null) {
      classParent = jPackage;
    } else {
      JDefinedClass definedClass = javaCodeGenerator.get(parentSchema).getDefinedClass();
      classParent = definedClass == null ? jPackage : definedClass;
    }

    String name = nameForSchema(schema);
    boolean isComplexObject = dataType.equals(jCodeModel.ref(JSONObject.class))
        && !combinedSchema.getProperties().isEmpty();
    boolean isArray = dataType.equals(jCodeModel.ref(JSONArray.class));
    if (isComplexObject || dataType.equals(jCodeModel.ref(Object.class)) || isArray) {
      JDefinedClass _class = makeClassForSchema(name,
          (name12)
              -> classParent._class(
                  parentSchema == null ? JMod.PUBLIC : JMod.STATIC | JMod.PUBLIC, name12));
      jDefinedClass = _class;
      _name = _class.name();

      StringBuilder docs = new StringBuilder();
      String toAppend = "";
      String description = schema.getDescription();
      if (description != null && !description.isEmpty()) {
        docs.append(htmlEscape(description));
        toAppend = System.lineSeparator() + System.lineSeparator();
      }
      String str = schema.getUri().toString();
      if (!str.isEmpty()) {
        docs.append(toAppend);
        docs.append("Created from ").append(str);
        toAppend = System.lineSeparator();
      }
      if (false) {
        docs.append(toAppend)
            .append("Explicit types ")
            .append(schema.getExplicitTypes())
            .append(System.lineSeparator())
            .append("Inferred types ")
            .append(combinedSchema.getInferredTypes());
      }

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

      for (Map.Entry<String, Schema> entry : combinedSchema.getProperties().entrySet()) {
        Schema propertySchema = entry.getValue();
        JavaBuilder javaBuilder = javaCodeGenerator.get(propertySchema);
        String propertyName = entry.getKey();
        javaBuilder.writePropertyGetters(schema.getRequiredProperties().contains(propertyName),
            expressionFromObject(propertySchema.getDefault()), jDefinedClass, dataField,
            propertyName, jCodeModel);
      }

      Collection<Schema> itemsTuple = schema.getItemsTuple();
      if (itemsTuple != null) {
        int idx = 0;
        for (Schema itemsSchema : itemsTuple) {
          JavaBuilder javaBuilder = javaCodeGenerator.get(itemsSchema);
          javaBuilder.writeItemGetters(jDefinedClass, idx, dataField, jCodeModel,
              expressionFromObject(itemsSchema.getDefault()));
          idx++;
        }
      }

      Schema _items = schema.getItems();
      if (_items != null) {
        JavaBuilder javaBuilder = javaCodeGenerator.get(_items);
        javaBuilder.writeItemGetters(
            jDefinedClass, -1, dataField, jCodeModel, expressionFromObject(_items.getDefault()));
      }

      Schema additionalItems = schema.getAdditionalItems();
      if (additionalItems != null) {
        JavaBuilder javaBuilder = javaCodeGenerator.get(additionalItems);
        javaBuilder.writeItemGetters(jDefinedClass, -1, dataField, jCodeModel,
            expressionFromObject(additionalItems.getDefault()));
      }

      if (types.contains("array")) {
        jDefinedClass.method(JMod.PUBLIC, jCodeModel.INT, "size")
            .body()
            ._return(JExpr.invoke(
                castIfNeeded(jCodeModel.ref(JSONArray.class), dataField.type(), dataField),
                "length"));
      }
    } else if (schema.getEnums() != null && dataType.equals(jCodeModel.ref(String.class))) {
      List<Object> enums = schema.getEnums();
      JDefinedClass _enum = makeClassForSchema(name, classParent::_enum);
      String s = schema.getUri().toString();
      if (!s.isEmpty()) {
        _enum.javadoc().add("Created from " + s);
      }
      _name = _enum.name();
      for (Object value : enums) {
        enumConstants.add(
            _enum.enumConstant(makeJavaLegal(camelToSnake(value.toString()).toUpperCase())));
      }
      jDefinedClass = _enum;
    } else {
      jDefinedClass = null;
      _name = name;
    }
  }

  private static String htmlEscape(String description) {
    return description.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");
  }

  private static IJExpression expressionFromObject(Object object) {
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

  private static IJExpression castIfNeeded(
      AbstractJType requiredType, AbstractJType sourceType, IJExpression source) {
    return sourceType.equals(requiredType) ? source : source.castTo(requiredType);
  }

  private static String getOptOrGet(boolean get, AbstractJType dataType, JCodeModel jCodeModel) {
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

  private void writePropertyGetters(boolean requiredProperty, IJExpression defaultValue,
      JDefinedClass holderClass, JFieldVar dataField, String propertyName, JCodeModel jCodeModel) {
    IJExpression asJsonObject =
        castIfNeeded(jCodeModel.ref(JSONObject.class), dataField.type(), dataField);
    String nameForGetters = NameUtils.snakeToCamel(propertyName);
    AbstractJType returnType = jDefinedClass == null ? dataType : jDefinedClass;
    JMethod getter = holderClass.method(JMod.PUBLIC, returnType,
        (returnType.equals(jCodeModel.BOOLEAN) ? "is" : "get") + nameForGetters);
    boolean isGet = defaultValue == null;
    JInvocation getObject =
        JExpr.invoke(asJsonObject, getOptOrGet(isGet, dataType, jCodeModel)).arg(propertyName);
    if (defaultValue != null && !defaultValue.equals(JExpr.lit(false))) {
      getObject.arg(defaultValue);
    }
    JBlock body = getter.body();
    makeReturn(jCodeModel, dataType, getObject, body);

    if (!requiredProperty && isGet) {
      JMethod has = holderClass.method(JMod.PUBLIC, jCodeModel.BOOLEAN, "has" + nameForGetters);
      has.body()._return(JExpr.invoke(asJsonObject, "has").arg(propertyName));
    }
  }

  private void makeReturn(
      JCodeModel jCodeModel, AbstractJType sourceType, JInvocation source, JBlock body) {
    AbstractJType returnType = jDefinedClass == null ? dataType : jDefinedClass;
    if (jDefinedClass == null) {
      IJExpression toReturn;
      if (sourceType.equals(returnType)) {
        toReturn = source;
      } else if (returnType.equals(jCodeModel.LONG)) {
        toReturn =
            castIfNeeded(jCodeModel.ref(Number.class), sourceType, source).invoke("longValue");
      } else if (returnType.equals(jCodeModel.INT)) {
        toReturn =
            castIfNeeded(jCodeModel.ref(Number.class), sourceType, source).invoke("intValue");
      } else {
        toReturn = source.castTo(returnType);
      }
      body._return(toReturn);
    } else if (!enumConstants.isEmpty()) {
      JVar value = body.decl(jCodeModel.ref(String.class), "value").init(source);
      List<Object> enums = schema.getEnums();
      JSwitch jSwitch = body._switch(value);
      for (int idx = 0; idx != enums.size(); idx++) {
        jSwitch._case(expressionFromObject(enums.get(idx))).body()._return(enumConstants.get(idx));
      }
      body._throw(JExpr._new(jCodeModel.ref(IllegalStateException.class))
                      .arg(JExpr.lit("Unexpected enum ").plus(value)));
    } else {
      body._return(JExpr._new(jDefinedClass).arg(castIfNeeded(dataType, sourceType, source)));
    }
  }

  private void writeItemGetters(JDefinedClass holderClass, int fixedPosition, JFieldVar dataField,
      JCodeModel jCodeModel, IJExpression defaultValue) {
    IJExpression asJsonArray =
        castIfNeeded(jCodeModel.ref(JSONArray.class), dataField.type(), dataField);
    String nameForGetters = _name;
    AbstractJType returnType = jDefinedClass == null ? dataType : jDefinedClass;
    JMethod getter = holderClass.method(JMod.PUBLIC, returnType,
        (returnType.equals(jCodeModel.BOOLEAN) ? "is" : "get") + nameForGetters);
    IJExpression positionSource;
    if (fixedPosition == -1) {
      positionSource = getter.param(jCodeModel.INT, "index");
    } else {
      positionSource = JExpr.lit(fixedPosition);
    }

    boolean isGet = defaultValue == null;
    JInvocation getObject =
        JExpr.invoke(asJsonArray, getOptOrGet(isGet, dataType, jCodeModel)).arg(positionSource);
    if (defaultValue != null && !defaultValue.equals(JExpr.lit(false))) {
      getObject.arg(defaultValue);
    }
    if (jDefinedClass == null) {
      getter.body()._return(getObject);
    } else {
      getter.body()._return(JExpr._new(jDefinedClass).arg(getObject));
    }

    holderClass._implements(jCodeModel.ref(Iterable.class).narrow(returnType));
    AbstractJClass iteratorType = jCodeModel.ref(Iterator.class).narrow(returnType);
    JMethod iteratorMethod = holderClass.method(JMod.PUBLIC, iteratorType, "iterator");
    JDefinedClass iteratorAnonClass = jCodeModel.anonymousClass(iteratorType);
    JVar nativeIterator =
        iteratorMethod.body().decl(jCodeModel.ref(Iterator.class).narrow(Object.class), "iterator",
            JExpr.invoke(asJsonArray, "iterator"));
    iteratorAnonClass.method(JMod.PUBLIC, jCodeModel.BOOLEAN, "hasNext")
        .body()
        ._return(JExpr.invoke(nativeIterator, "hasNext"));

    makeReturn(jCodeModel, jCodeModel.ref(Object.class), JExpr.invoke(nativeIterator, "next"),
        iteratorAnonClass.method(JMod.PUBLIC, returnType.boxify(), "next").body());
    iteratorMethod.body()._return(JExpr._new(iteratorAnonClass));
  }
}
