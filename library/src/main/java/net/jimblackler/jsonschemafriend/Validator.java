package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ComparableMutable.makeComparable;
import static net.jimblackler.jsonschemafriend.Utils.setOf;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Validator {
  public static void validate(
      Schema schema, Object document, URI uri, Consumer<ValidationError> errorConsumer) {
    if (schema.isFalse()) {
      errorConsumer.accept(new ValidationError(uri, document, "Failed", schema));
      return;
    }
    Object object;
    String query = uri.getQuery();
    if (query == null || query.isEmpty()) {
      try {
        object = PathUtils.fetchFromPath(document, uri.getRawFragment());
      } catch (MissingPathException e) {
        errorConsumer.accept(new ValidationError(
            uri, document, "Could not locate " + uri + " : " + e.getMessage(), schema));
        return;
      }
    } else {
      // Query part can carry a string for validation while preserving the rest of the URI for error
      // messages. This is used for propertyName validation where it's not possible to link to the
      // name with a standard JSON Pointer.
      object = query;
    }

    // object = rewriteObject(object);

    Number multipleOf = schema.getMultipleOf();
    Number minimum = schema.getMinimum();
    Number maximum = schema.getMaximum();
    Number exclusiveMinimum = schema.getExclusiveMinimum();
    Number exclusiveMaximum = schema.getExclusiveMaximum();
    Boolean exclusiveMinimumBoolean = schema.getExclusiveMinimumBoolean();
    Boolean exclusiveMaximumBoolean = schema.getExclusiveMaximumBoolean();
    Collection<String> disallow = schema.getDisallow();

    if (object instanceof Number) {
      Number number = (Number) object;
      if (multipleOf != null && number.doubleValue() / multipleOf.doubleValue() % 1 != 0) {
        errorConsumer.accept(new NotAMultipleError(uri, document, multipleOf, schema));
      }
      if (maximum != null
          && (exclusiveMaximumBoolean ? number.doubleValue() >= maximum.doubleValue()
                                      : number.doubleValue() > maximum.doubleValue())) {
        errorConsumer.accept(
            new GreaterThanMaximumError(uri, document, exclusiveMaximumBoolean, maximum, schema));
      }

      if (exclusiveMaximum != null && number.doubleValue() >= exclusiveMaximum.doubleValue()) {
        errorConsumer.accept(new ValidationError(
            uri, document, "Greater than or equal to exclusive maximum", schema));
      }
      if (minimum != null) {
        if (exclusiveMinimumBoolean ? number.doubleValue() <= minimum.doubleValue()
                                    : number.doubleValue() < minimum.doubleValue()) {
          errorConsumer.accept(
              new LessThanMinimumError(uri, document, exclusiveMinimumBoolean, minimum, schema));
        }
      }
      if (exclusiveMinimum != null && number.doubleValue() <= exclusiveMinimum.doubleValue()) {
        errorConsumer.accept(
            new ValidationError(uri, document, "Less than or equal to exclusive minimum", schema));
      }
      Set<String> okTypes = new HashSet<>();
      okTypes.add("number");
      if (new BigDecimal(number.toString()).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO)
          == 0) {
        okTypes.add("integer");
      }

      typeCheck(schema, document, uri, okTypes, disallow, errorConsumer);

      Number divisibleBy = schema.getDivisibleBy();
      if (divisibleBy != null && number.doubleValue() / divisibleBy.doubleValue() % 1 != 0) {
        errorConsumer.accept(new ValidationError(uri, document, "divisibleBy failed", schema));
      }

    } else if (object instanceof String) {
      String string = (String) object;
      int unicodeCompliantLength = string.codePointCount(0, string.length());
      Number minLength = schema.getMinLength();
      Number maxLength = schema.getMaxLength();
      if (maxLength != null && unicodeCompliantLength > maxLength.intValue()) {
        errorConsumer.accept(new ValidationError(uri, document, "Longer than maxLength", schema));
      }
      if (minLength != null && unicodeCompliantLength < minLength.intValue()) {
        errorConsumer.accept(new ValidationError(uri, document, "Shorter than minLength", schema));
      }
      Ecma262Pattern pattern = schema.getPattern();
      if (pattern != null && !pattern.matches(string)) {
        errorConsumer.accept(
            new ValidationError(uri, document, "Did not match pattern: " + pattern, schema));
      }
      typeCheck(schema, document, uri, setOf("string"), disallow, errorConsumer);
    } else if (object instanceof Boolean) {
      typeCheck(schema, document, uri, setOf("boolean"), disallow, errorConsumer);
    } else if (object instanceof JSONArray) {
      typeCheck(schema, document, uri, setOf("array"), disallow, errorConsumer);
      JSONArray jsonArray = (JSONArray) object;
      List<Schema> itemsTuple = schema.getItemsTuple();
      Schema additionalItems = schema.getAdditionalItems();
      if (itemsTuple != null) {
        if (jsonArray.length() > itemsTuple.size() && additionalItems != null) {
          for (int idx = itemsTuple.size(); idx != jsonArray.length(); idx++) {
            validate(additionalItems, document, PathUtils.append(uri, String.valueOf(idx)),
                errorConsumer);
          }
        }

        for (int idx = 0; idx != Math.min(itemsTuple.size(), jsonArray.length()); idx++) {
          validate(itemsTuple.get(idx), document, PathUtils.append(uri, String.valueOf(idx)),
              errorConsumer);
        }
      }

      Schema _items = schema.getItems();

      if (_items != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          validate(_items, document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer);
        }
      }

      Number maxItems = schema.getMaxItems();
      if (maxItems != null && jsonArray.length() > maxItems.intValue()) {
        errorConsumer.accept(new AboveMaxItemsValidationError(uri, document, maxItems, schema));
      }

      Number minItems = schema.getMinItems();
      if (minItems != null && jsonArray.length() < minItems.intValue()) {
        errorConsumer.accept(new BelowMinItemsValidationError(uri, document, minItems, schema));
      }

      boolean uniqueItems = schema.getUniqueItems();
      if (uniqueItems) {
        Collection<Object> items = new HashSet<>();
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          if (!items.add(makeComparable(jsonArray.get(idx)))) {
            errorConsumer.accept(
                new ValidationError(uri, document, "Non-unique item found", schema));
          }
        }
      }

      Schema contains = schema.getContains();
      if (contains != null) {
        boolean onePassed = false;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          validate(contains, document, PathUtils.append(uri, String.valueOf(idx)), errors::add);
          if (errors.isEmpty()) {
            onePassed = true;
            break;
          }
        }
        if (!onePassed) {
          errorConsumer.accept(new ValidationError(
              uri, document, "No element in the array matched contains", schema));
        }
      }
    } else if (object instanceof JSONObject) {
      typeCheck(schema, document, uri, setOf("object"), disallow, errorConsumer);
      JSONObject jsonObject = (JSONObject) object;
      Number maxProperties = schema.getMaxProperties();
      if (maxProperties != null && jsonObject.length() > maxProperties.intValue()) {
        errorConsumer.accept(new ValidationError(uri, document, "Too mamy properties", schema));
      }
      Number minProperties = schema.getMinProperties();
      if (minProperties != null && jsonObject.length() < minProperties.intValue()) {
        errorConsumer.accept(new ValidationError(uri, document, "Too few properties", schema));
      }

      Collection<String> requiredProperties = schema.getRequiredProperties();
      for (String property : requiredProperties) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(new MissingPropertyError(uri, document, property, schema));
        }
      }

      Map<String, Schema> _properties = schema.getProperties();
      Collection<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      Collection<Ecma262Pattern> patternPropertiesPatterns = schema.getPatternPropertiesPatterns();
      Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          validate(
              _properties.get(property), document, PathUtils.append(uri, property), errorConsumer);
          remainingProperties.remove(property);
        }

        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchema.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern1 = it0.next();
          Schema schema1 = it1.next();
          if (pattern1.matches(property)) {
            validate(schema1, document, PathUtils.append(uri, property), errorConsumer);
            remainingProperties.remove(property);
          }
        }
        Schema propertyNames = schema.getPropertyNames();
        if (propertyNames != null) {
          try {
            // To provide developer-friendly validation error messages, the validator takes a URL to
            // the object being validated, relative to the base document. In turn, to avoid
            // redundant coupled parameters, the object is not passed as a parameter but converted
            // to the object inside the validator. This is a problem for propertyName validation
            // because the property name itself cannot have a path using the current version of JSON
            // Pointers. Relative JSON Pointers does support property names; but the standard states
            // these pointers are not suitable for use in URIs. As a workaround we use the query
            // part of the URL to carry the property name into the child iteration of the validator.
            URI propertyPath = new URI(
                uri.getScheme(), uri.getAuthority(), uri.getPath(), property, uri.getRawFragment());
            validate(propertyNames, document, propertyPath, errorConsumer);
          } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      Map<String, Schema> schemaDependencies = schema.getSchemaDependencies();
      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }
        validate(entry.getValue(), document, uri, errorConsumer);
      }

      Schema additionalProperties = schema.getAdditionalProperties();
      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          validate(additionalProperties, document, PathUtils.append(uri, property), errorConsumer);
        }
      }
      Map<String, Collection<String>> dependencies = schema.getDependencies();
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
          errorConsumer.accept(new ValidationError(
              uri, document, "Missing dependency " + property + " -> " + dependency, schema));
        }
      }

    } else if (object == JSONObject.NULL) {
      typeCheck(schema, document, uri, setOf("null"), disallow, errorConsumer);
    } else if (object != null) {
      errorConsumer.accept(new ValidationError(
          uri, document, "Cannot validate type " + object.getClass().getSimpleName(), schema));
    }

    Object _const = schema.getConst();
    if (_const != null) {
      if (!makeComparable(_const).equals(makeComparable(object))) {
        errorConsumer.accept(new ConstMismatchError(uri, document, _const, schema));
      }
    }

    List<Object> enums = schema.getEnums();
    if (enums != null) {
      boolean matchedOne = false;
      Object o = makeComparable(object);
      for (Object value : enums) {
        if (o.equals(makeComparable(value))) {
          matchedOne = true;
          break;
        }
      }
      if (!matchedOne) {
        errorConsumer.accept(new ValidationError(uri, document, "Object not in enum", schema));
      }
    }

    Schema _if = schema.getIf();
    Schema _then = schema.getThen();
    Schema _else = schema.getElse();

    if (_if != null) {
      List<ValidationError> errors = new ArrayList<>();
      validate(_if, document, uri, errors::add);
      Schema useSchema;
      if (errors.isEmpty()) {
        useSchema = _then;
      } else {
        useSchema = _else;
      }
      if (useSchema != null) {
        validate(useSchema, document, uri, errorConsumer);
      }
    }

    Collection<Schema> allOf = schema.getAllOf();
    for (Schema schema1 : allOf) {
      validate(schema1, document, uri, errorConsumer);
    }

    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null) {
      boolean onePassed = false;
      List<List<ValidationError>> allErrors = new ArrayList<>();
      for (Schema schema1 : anyOf) {
        List<ValidationError> errors = new ArrayList<>();
        validate(schema1, document, uri, errors::add);
        if (errors.isEmpty()) {
          onePassed = true;
          break;
        }
        allErrors.add(errors);
      }
      if (!onePassed) {
        errorConsumer.accept(new AnyOfValidationError(uri, document, allErrors, schema));
      }
    }

    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null) {
      int numberPassed = 0;
      List<List<ValidationError>> allErrors = new ArrayList<>();
      for (Schema schema1 : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        validate(schema1, document, uri, errors::add);
        if (errors.isEmpty()) {
          numberPassed++;
        }
        allErrors.add(errors);
      }
      if (numberPassed != 1) {
        errorConsumer.accept(
            new OneOfValidationError(uri, document, numberPassed, allErrors, schema));
      }
    }

    Schema not = schema.getNot();
    if (not != null) {
      List<ValidationError> errors = new ArrayList<>();
      validate(not, document, uri, errors::add);
      if (errors.isEmpty()) {
        errorConsumer.accept(new NotValidationError(uri, document, schema));
      }
    }

    Collection<Schema> disallowSchemas = schema.getDisallowSchemas();
    for (Schema disallowSchema : disallowSchemas) {
      List<ValidationError> errors = new ArrayList<>();
      validate(disallowSchema, document, uri, errors::add);
      if (errors.isEmpty()) {
        errorConsumer.accept(
            new ValidationError(uri, document, "disallow condition passed", schema));
      }
    }
  }

  private static void typeCheck(Schema schema, Object document, URI path, Set<String> types,
      Collection<String> disallow, Consumer<ValidationError> errorConsumer) {
    Collection<String> typesIn0 = new HashSet<>(types);
    typesIn0.retainAll(disallow);
    if (!typesIn0.isEmpty()) {
      errorConsumer.accept(new ValidationError(path, document, "Type disallowed", schema));
    }

    Collection<String> explicitTypes = schema.getExplicitTypes();

    if (explicitTypes == null) {
      return;
    }

    Collection<Schema> typesSchema = schema.getTypesSchema();

    for (Schema schema1 : typesSchema) {
      List<ValidationError> errors = new ArrayList<>();
      validate(schema1, document, path, errors::add);
      if (errors.isEmpty()) {
        return;
      }
    }

    if (explicitTypes.contains("any")) {
      return;
    }

    Collection<String> typesIn = new HashSet<>(types);
    typesIn.retainAll(explicitTypes);
    if (!typesIn.isEmpty()) {
      return;
    }

    errorConsumer.accept(new ValidationError(path, document,
        "Expected: [" + String.join(", ", explicitTypes) + "] "
            + "Found: [" + String.join(", ", types) + "]",
        schema));
  }

  public static void validate(Schema schema, Object document) throws ValidationException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, document, errors::add);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors.toString());
    }
  }

  public static void validate(
      Schema schema, Object document, Consumer<ValidationError> errorConsumer) {
    validate(schema, document, URI.create(""), errorConsumer);
  }

  private static Object rewriteObject(Object object) {
    if (!(object instanceof String)) {
      return object;
    }
    String string = (String) object;
    // Reverse what org.json does to long numbers (converts them into strings).
    // Strings are rewritten as a number in the cases where a string would have been the only way to
    // serialize the number.
    // It would be better to have a JSON deserializer that used BigInteger where necessary.
    // But this won't be added to org.json soon: https://github.com/stleary/JSON-java/issues/157
    try {
      JSONArray testObject = new JSONArray(String.format("[%s]", string));
      if (testObject.get(0) instanceof String) {
        BigInteger bigInteger = new BigInteger(string);
        if (bigInteger.toString().equals(string)) {
          return bigInteger;
        }
      }
      return object;
    } catch (NumberFormatException | JSONException e) {
      // Doesn't look like a number after all.
      return object;
    }
  }
}
