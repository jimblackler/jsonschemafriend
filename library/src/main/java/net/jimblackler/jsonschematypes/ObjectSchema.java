package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschematypes.JsonSchemaRef.append;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

public class ObjectSchema extends Schema {
  private final Map<String, Schema> _properties = new HashMap<>();
  private final Collection<Ecma262Pattern> patternPropertiesPatterns = new ArrayList<>();
  private final Collection<Schema> patternPropertiesSchemas = new ArrayList<>();
  private final Set<String> required = new HashSet<>();
  private final Collection<Schema> itemsArray = new ArrayList<>();
  private final Schema itemsSingle;
  private final Collection<Schema> allOf;
  private final Collection<Schema> anyOf;
  private final Collection<Schema> oneOf;
  private final Set<String> explicitTypes;
  private final Double minimum;
  private final Double maximum;
  private final Double exclusiveMinimum;
  private final Double exclusiveMaximum;
  private final Double multipleOf;
  private final Integer minLength;
  private final Integer maxLength;
  private final int minProperties;
  private final Schema additionalProperties;
  private final Object _const;
  private final Schema contains;
  private final Map<String, Collection<String>> dependencies = new HashMap<>();
  private final Map<String, Schema> schemaDependencies = new HashMap<>();

  public ObjectSchema(SchemaStore schemaStore, URI path) throws GenerationException {
    super(schemaStore, path);
    JSONObject jsonObject = (JSONObject) schemaStore.getSchemaJson(path);
    if (jsonObject == null) {
      throw new GenerationException("Could not obtain " + path);
    }

    // Get explicit types.
    Object type = jsonObject.opt("type");
    if (type instanceof JSONArray) {
      explicitTypes = new HashSet<>();
      JSONArray array = (JSONArray) type;
      for (int idx = 0; idx != array.length(); idx++) {
        explicitTypes.add(array.getString(idx));
      }
    } else if (type instanceof String) {
      explicitTypes = Set.of(type.toString());
    } else {
      explicitTypes = null;
    }

    // Get properties.
    if (jsonObject.has("properties")) {
      JSONObject properties = jsonObject.getJSONObject("properties");
      URI propertiesPointer = append(path, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        _properties.put(
            propertyName, schemaStore.getSchema(append(propertiesPointer, propertyName)));
      }
    }

    if (jsonObject.has("patternProperties")) {
      JSONObject patternProperties = jsonObject.getJSONObject("patternProperties");
      URI propertiesPointer = append(path, "patternProperties");
      Iterator<String> it = patternProperties.keys();
      while (it.hasNext()) {
        String propertyPattern = it.next();
        patternPropertiesPatterns.add(new Ecma262Pattern(propertyPattern));
        patternPropertiesSchemas.add(
            schemaStore.getSchema(append(propertiesPointer, propertyPattern)));
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
    if (jsonObject.has("additionalProperties")) {
      additionalProperties = schemaStore.getSchema(append(path, "additionalProperties"));
    } else {
      additionalProperties = null;
    }

    if (jsonObject.has("required")) {
      JSONArray array = jsonObject.getJSONArray("required");
      for (int idx = 0; idx != array.length(); idx++) {
        required.add(array.getString(idx));
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.opt("items");
    URI itemsPath = append(path, "items");
    if (items instanceof JSONObject || items instanceof Boolean) {
      itemsSingle = schemaStore.getSchema(itemsPath);
    } else {
      itemsSingle = null;
      if (items instanceof JSONArray) {
        JSONArray jsonArray = (JSONArray) items;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          itemsArray.add(schemaStore.getSchema(append(itemsPath, String.valueOf(idx))));
        }
      }
    }

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.4
    if (jsonObject.has("contains")) {
      contains = schemaStore.getSchema(append(path, "contains"));
    } else {
      contains = null;
    }

    if (jsonObject.has("allOf")) {
      allOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("allOf");
      URI arrayPath = append(path, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      allOf = null;
    }

    if (jsonObject.has("anyOf")) {
      anyOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("anyOf");
      URI arrayPath = append(path, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        anyOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      anyOf = null;
    }

    if (jsonObject.has("oneOf")) {
      oneOf = new ArrayList<>();
      JSONArray array = jsonObject.getJSONArray("oneOf");
      URI arrayPath = append(path, "oneOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        oneOf.add(schemaStore.getSchema(indexPointer));
      }
    } else {
      oneOf = null;
    }

    if (jsonObject.has("minimum")) {
      minimum = jsonObject.getDouble("minimum");
    } else {
      minimum = null;
    }

    if (jsonObject.has("maximum")) {
      maximum = jsonObject.getDouble("maximum");
    } else {
      maximum = null;
    }

    if (jsonObject.has("exclusiveMinimum")) {
      exclusiveMinimum = jsonObject.getDouble("exclusiveMinimum");
    } else {
      exclusiveMinimum = null;
    }

    if (jsonObject.has("exclusiveMaximum")) {
      exclusiveMaximum = jsonObject.getDouble("exclusiveMaximum");
    } else {
      exclusiveMaximum = null;
    }

    if (jsonObject.has("multipleOf")) {
      multipleOf = jsonObject.getDouble("multipleOf");
    } else {
      multipleOf = null;
    }

    if (jsonObject.has("minLength")) {
      minLength = jsonObject.getInt("minLength");
    } else {
      minLength = null;
    }

    if (jsonObject.has("maxLength")) {
      maxLength = jsonObject.getInt("maxLength");
    } else {
      maxLength = null;
    }

    if (jsonObject.has("minProperties")) {
      minProperties = jsonObject.getInt("minProperties");
    } else {
      minProperties = 0;
    }

    if (jsonObject.has("const")) {
      _const = jsonObject.get("const");
    } else {
      _const = null;
    }

    if (jsonObject.has("dependencies")) {
      JSONObject dependenciesObject = jsonObject.getJSONObject("dependencies");
      for (String dependency : dependenciesObject.keySet()) {
        List<String> spec = new ArrayList<>();
        Object dependencyObject = dependenciesObject.get(dependency);
        if (dependencyObject instanceof JSONArray) {
          JSONArray array = (JSONArray) dependencyObject;
          for (int idx = 0; idx != array.length(); idx++) {
            spec.add(array.getString(idx));
          }
          dependencies.put(dependency, spec);
        } else {
          URI dependenciesPpinter = append(path, "dependencies");
          schemaDependencies.put(
              dependency, schemaStore.getSchema(append(dependenciesPpinter, dependency)));
        }
      }
    }
  }

  @Override
  public void validate(Object object, Consumer<ValidationError> errorConsumer) {
    if (object instanceof Number) {
      if (object instanceof Integer) {
        if (explicitTypes != null && !explicitTypes.contains("integer")
            && !explicitTypes.contains("number")) {
          errorConsumer.accept(new ValidationError("Type mismatch"));
        }
      } else if (explicitTypes != null && !explicitTypes.contains("number")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
      Number number = (Number) object;
      if (minimum != null) {
        if (number.doubleValue() < minimum) {
          errorConsumer.accept(new ValidationError("Less than minimum"));
        }
      }
      if (exclusiveMinimum != null) {
        if (number.doubleValue() <= exclusiveMinimum) {
          errorConsumer.accept(new ValidationError("Less than or equal to exclusive minimum"));
        }
      }
      if (maximum != null) {
        if (number.doubleValue() > maximum) {
          errorConsumer.accept(new ValidationError("Greater than maximum"));
        }
      }
      if (exclusiveMaximum != null) {
        if (number.doubleValue() >= exclusiveMaximum) {
          errorConsumer.accept(new ValidationError("Greater than or equal to exclusive maximum"));
        }
      }
      if (multipleOf != null) {
        if (number.doubleValue() / multipleOf % 1 != 0) {
          errorConsumer.accept(new ValidationError("Not a multiple"));
        }
      }
    } else if (object instanceof Boolean) {
      if (explicitTypes != null && !explicitTypes.contains("boolean")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
    } else if (object instanceof String) {
      if (explicitTypes != null && !explicitTypes.contains("string")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
      String string = (String) object;
      if (minLength != null) {
        if (string.length() < minLength) {
          errorConsumer.accept(new ValidationError("Shorter than minLength"));
        }
      }
      if (maxLength != null) {
        if (string.length() > maxLength) {
          errorConsumer.accept(new ValidationError("Longer than maxLength"));
        }
      }
    } else if (object instanceof JSONArray) {
      if (explicitTypes != null && !explicitTypes.contains("array")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
      if (itemsSingle != null) {
        JSONArray jsonArray = (JSONArray) object;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          itemsSingle.validate(jsonArray.get(idx), errorConsumer);
        }
      }

      if (contains != null) {
        JSONArray jsonArray = (JSONArray) object;
        boolean onePassed = false;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          contains.validate(jsonArray.get(idx), errors::add);
          if (errors.isEmpty()) {
            onePassed = true;
            break;
          }
        }
        if (!onePassed) {
          errorConsumer.accept(new ValidationError("No element in the array matched contains"));
        }
      }
    } else if (object instanceof JSONObject) {
      if (explicitTypes != null && !explicitTypes.contains("object")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
      JSONObject jsonObject = (JSONObject) object;
      if (jsonObject.length() < minProperties) {
        errorConsumer.accept(new ValidationError("Too few properties"));
      }
      Set<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          Schema schema = _properties.get(property);
          schema.validate(jsonObject.get(property), errorConsumer);
          remainingProperties.remove(property);
        }
        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchemas.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern = it0.next();
          Schema schema = it1.next();
          if (pattern.matches(property)) {
            schema.validate(jsonObject.get(property), errorConsumer);
            remainingProperties.remove(property);
          }
        }
      }
      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          additionalProperties.validate(jsonObject.get(property), errorConsumer);
        }
      }

      for (String property : required) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(new ValidationError("Missing required property " + property));
        }
      }

      for (Map.Entry<String, Collection<String>> entry : dependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }

        Collection<String> _dependencies = entry.getValue();
        for (String dependency : _dependencies) {
          if (jsonObject.has(dependency)) {
            continue;
          }
          errorConsumer.accept(
              new ValidationError("Missing dependency " + property + " -> " + dependency));
        }
      }

      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }

        Schema schema = entry.getValue();
        schema.validate(jsonObject, errorConsumer);
      }
    }

    if (_const != null) {
      if (_const instanceof Integer && object instanceof Integer) {
        if (!_const.equals(object)) {
          errorConsumer.accept(new ValidationError("const integers didn't match"));
        }
      } else if (_const instanceof Number && object instanceof Number) {
        Number constNumber = (Number) _const;
        Number objectNumber = (Number) object;
        if (constNumber.doubleValue() != objectNumber.doubleValue()) {
          errorConsumer.accept(new ValidationError("const numbers didn't match"));
        }
      } else if ((_const instanceof JSONObject && object instanceof JSONObject)
          || (_const instanceof JSONArray && object instanceof JSONArray)) {
        if (!_const.toString().equals(object.toString())) {
          errorConsumer.accept(new ValidationError("const json types didn't match"));
        }
      } else {
        if (!_const.equals(object)) {
          errorConsumer.accept(new ValidationError("const didn't match"));
        }
      }
    } else if (object == JSONObject.NULL) {
      if (explicitTypes != null && !explicitTypes.contains("null")) {
        errorConsumer.accept(new ValidationError("Type mismatch"));
      }
    }

    if (allOf != null) {
      for (Schema schema : allOf) {
        schema.validate(object, errorConsumer);
      }
    }

    if (anyOf != null) {
      boolean onePassed = false;
      for (Schema schema : anyOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(object, errors::add);
        if (errors.isEmpty()) {
          onePassed = true;
          break;
        }
      }
      if (!onePassed) {
        errorConsumer.accept(new ValidationError("All anyOf failed"));
      }
    }

    if (oneOf != null) {
      int numberPassed = 0;
      for (Schema schema : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(object, errors::add);
        if (errors.isEmpty()) {
          numberPassed++;
        }
      }
      if (numberPassed != 1) {
        errorConsumer.accept(new ValidationError(numberPassed + " passed oneOf"));
      }
    }
  }
}
