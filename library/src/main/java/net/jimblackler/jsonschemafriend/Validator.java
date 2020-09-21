package net.jimblackler.jsonschemafriend;

import static java.util.Base64.getUrlDecoder;
import static net.jimblackler.jsonschemafriend.ComparableMutable.makeComparable;
import static net.jimblackler.jsonschemafriend.DocumentUtils.loadJson;
import static net.jimblackler.jsonschemafriend.MetaSchemaDetector.detectMetaSchema;
import static net.jimblackler.jsonschemafriend.Utils.setOf;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
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
    validate(schema, document, uri, errorConsumer, property -> {}, item -> {}, null);
  }

  public static void validate(Schema schema, Object document, URI uri,
      Consumer<ValidationError> errorConsumer, Schema recursiveRef) {
    validate(schema, document, uri, errorConsumer, property -> {}, item -> {}, recursiveRef);
  }

  public static void validate(Schema schema, Object document, URI uri,
      Consumer<ValidationError> errorConsumer, Consumer<String> propertyConsumer,
      Consumer<Integer> itemConsumer, Schema recursiveRef) {
    Object object;
    String query = uri.getQuery();
    if (query == null || query.isEmpty()) {
      object = PathUtils.fetchFromPath(document, uri.getRawFragment());
    } else {
      // Query part can carry a string for validation while preserving the rest of the URI for error
      // messages. This is used for propertyName validation where it's not possible to link to the
      // name with a standard JSON Pointer.
      object = query;
    }

    if (schema.isFalse()) {
      errorConsumer.accept(new FalseSchemaError(uri, document, schema));
      return;
    }

    Collection<String> evaluatedProperties = new HashSet<>();
    Consumer<String> selfPropertyHandler = property -> {
      propertyConsumer.accept(property);
      evaluatedProperties.add(property);
    };

    Collection<Integer> evaluatedItems = new HashSet<>();
    Consumer<Integer> selfItemHandler = property -> {
      itemConsumer.accept(property);
      evaluatedItems.add(property);
    };

    Schema recursiveRef1 = schema.getRecursiveRef();
    if (recursiveRef1 != null) {
      if (recursiveRef != null && recursiveRef1.isRecursiveAnchor()) {
        validate(
            recursiveRef, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler, null);
      } else {
        validate(recursiveRef1, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler,
            null);
      }
    }

    if (recursiveRef == null && schema.isRecursiveAnchor()) {
      recursiveRef = schema;
    }

    Schema _if = schema.getIf();
    Schema _then = schema.getThen();
    Schema _else = schema.getElse();

    if (_if != null) {
      List<ValidationError> errors = new ArrayList<>();
      Collection<String> unevaluatedProperties = new HashSet<>();
      Collection<Integer> unevaluatedItems = new HashSet<>();
      validate(_if, document, uri, errors::add, unevaluatedProperties::add, unevaluatedItems::add,
          recursiveRef);
      Schema useSchema;
      if (errors.isEmpty()) {
        useSchema = _then;
        unevaluatedProperties.forEach(selfPropertyHandler);
        unevaluatedItems.forEach(selfItemHandler);
      } else {
        useSchema = _else;
      }
      if (useSchema != null) {
        validate(useSchema, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler,
            recursiveRef);
      }
    }

    Schema ref = schema.getRef();
    if (ref != null) {
      validate(
          ref, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler, recursiveRef);
    }

    Collection<Schema> allOf = schema.getAllOf();
    for (Schema schema1 : allOf) {
      validate(schema1, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler,
          recursiveRef);
    }

    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null) {
      int numberPassed = 0;
      List<List<ValidationError>> allErrors = new ArrayList<>();
      for (Schema schema1 : anyOf) {
        List<ValidationError> errors = new ArrayList<>();
        Collection<String> unevaluatedProperties = new HashSet<>();
        Collection<Integer> unevaluatedItems = new HashSet<>();
        validate(schema1, document, uri, errors::add, unevaluatedProperties::add,
            unevaluatedItems::add, recursiveRef);
        if (errors.isEmpty()) {
          numberPassed++;
          unevaluatedProperties.forEach(selfPropertyHandler);
          unevaluatedItems.forEach(selfItemHandler);
        }
        allErrors.add(errors);
      }
      if (numberPassed == 0) {
        errorConsumer.accept(new AnyOfError(uri, document, allErrors, schema));
      }
    }

    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null) {
      List<Schema> passed = new ArrayList<>();
      List<List<ValidationError>> allErrors = new ArrayList<>();
      for (Schema schema1 : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        validate(schema1, document, uri, errors::add, selfPropertyHandler, selfItemHandler,
            recursiveRef);
        if (errors.isEmpty()) {
          passed.add(schema1);
        }
        allErrors.add(errors);
      }
      if (passed.size() != 1) {
        errorConsumer.accept(new OneOfError(uri, document, passed, allErrors, schema));
      }
    }

    Schema not = schema.getNot();
    if (not != null) {
      List<ValidationError> errors = new ArrayList<>();
      validate(not, document, uri, errors::add, recursiveRef);
      if (errors.isEmpty()) {
        errorConsumer.accept(new NotError(uri, document, schema));
      }
    }

    Collection<Schema> disallowSchemas = schema.getDisallowSchemas();
    for (Schema disallowSchema : disallowSchemas) {
      List<ValidationError> errors = new ArrayList<>();
      validate(disallowSchema, document, uri, errors::add, selfPropertyHandler, selfItemHandler,
          recursiveRef);
      if (errors.isEmpty()) {
        errorConsumer.accept(new DisallowError(uri, document, schema));
      }
    }

    Number multipleOf = schema.getMultipleOf();
    Number minimum = schema.getMinimum();
    Number maximum = schema.getMaximum();
    Number exclusiveMinimum = schema.getExclusiveMinimum();
    Number exclusiveMaximum = schema.getExclusiveMaximum();
    Boolean exclusiveMinimumBoolean = schema.isExclusiveMinimumBoolean();
    Boolean exclusiveMaximumBoolean = schema.isExclusiveMaximumBoolean();
    Collection<String> disallow = schema.getDisallow();

    if (object instanceof Number) {
      Number number = (Number) object;
      if (multipleOf != null && number.doubleValue() / multipleOf.doubleValue() % 1 != 0) {
        errorConsumer.accept(new MultipleError(uri, document, schema));
      }
      if (maximum != null
          && (exclusiveMaximumBoolean ? number.doubleValue() >= maximum.doubleValue()
                                      : number.doubleValue() > maximum.doubleValue())) {
        errorConsumer.accept(new MaximumError(uri, document, schema));
      }

      if (exclusiveMaximum != null && number.doubleValue() >= exclusiveMaximum.doubleValue()) {
        errorConsumer.accept(new ExclusiveMaximumError(uri, document, schema));
      }
      if (minimum != null) {
        if (exclusiveMinimumBoolean ? number.doubleValue() <= minimum.doubleValue()
                                    : number.doubleValue() < minimum.doubleValue()) {
          errorConsumer.accept(new MinimumError(uri, document, schema));
        }
      }
      if (exclusiveMinimum != null && number.doubleValue() <= exclusiveMinimum.doubleValue()) {
        errorConsumer.accept(new ExclusiveMinimumError(uri, document, schema));
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
        errorConsumer.accept(new DivisibleByError(uri, document, schema));
      }

    } else if (object instanceof String) {
      String string = (String) object;
      int unicodeCompliantLength = string.codePointCount(0, string.length());
      Number minLength = schema.getMinLength();
      Number maxLength = schema.getMaxLength();
      if (maxLength != null && unicodeCompliantLength > maxLength.intValue()) {
        errorConsumer.accept(new MaxLengthError(uri, document, schema));
      }
      if (minLength != null && unicodeCompliantLength < minLength.intValue()) {
        errorConsumer.accept(new MinLengthError(uri, document, schema));
      }
      Ecma262Pattern pattern = schema.getPattern();
      if (pattern != null && !pattern.matches(string)) {
        errorConsumer.accept(new PatternError(uri, document, schema));
      }
      String format = schema.getFormat();
      if (format != null) {
        URI metaSchema = detectMetaSchema(document);
        String message = FormatChecker.formatCheck(string, format, metaSchema);
        if (message != null) {
          errorConsumer.accept(new FormatError(uri, document, schema, message));
        }
      }
      String stringToValidate = string;
      String contentEncoding = schema.getContentEncoding();
      if (contentEncoding != null) {
        switch (contentEncoding) {
          case "base64":
            Base64.Decoder urlDecoder = getUrlDecoder();
            byte[] decoded = null;
            try {
              decoded = urlDecoder.decode(string);
            } catch (IllegalArgumentException e) {
              errorConsumer.accept(new ContentEncodingError(uri, document, schema, e.getMessage()));
            }
            if (decoded != null) {
              stringToValidate = new String(decoded, StandardCharsets.UTF_8);
            }

            break;
        }
      }

      String contentMediaType = schema.getContentMediaType();
      if (contentMediaType != null) {
        switch (contentMediaType) {
          case "application/json":
            try {
              new JSONArray(stringToValidate);
            } catch (JSONException e) {
              try {
                new JSONObject(stringToValidate);
              } catch (JSONException e2) {
                errorConsumer.accept(
                    new ContentEncodingError(uri, document, schema, e.getMessage()));
              }
            }
            break;
        }
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
                errorConsumer, recursiveRef);
            selfItemHandler.accept(idx);
          }
        }

        for (int idx = 0; idx != Math.min(itemsTuple.size(), jsonArray.length()); idx++) {
          validate(itemsTuple.get(idx), document, PathUtils.append(uri, String.valueOf(idx)),
              errorConsumer, recursiveRef);
          selfItemHandler.accept(idx);
        }
      }

      Schema _items = schema.getItems();
      if (_items != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          validate(_items, document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer,
              recursiveRef);
          selfItemHandler.accept(idx);
        }
      }

      Schema unevaluatedItems = schema.getUnevaluatedItems();
      if (unevaluatedItems != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          if (evaluatedItems.contains(idx)) {
            continue;
          }
          validate(unevaluatedItems, document, PathUtils.append(uri, String.valueOf(idx)),
              errorConsumer, recursiveRef);
          selfItemHandler.accept(idx);
        }
      }

      Number maxItems = schema.getMaxItems();
      if (maxItems != null && jsonArray.length() > maxItems.intValue()) {
        errorConsumer.accept(new MaxItemsError(uri, document, schema));
      }

      Number minItems = schema.getMinItems();
      if (minItems != null && jsonArray.length() < minItems.intValue()) {
        errorConsumer.accept(new MinItemsError(uri, document, schema));
      }

      if (schema.isUniqueItems()) {
        Collection<Object> items = new HashSet<>();
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          if (!items.add(makeComparable(jsonArray.get(idx)))) {
            errorConsumer.accept(new UniqueItemsError(uri, document, schema));
          }
        }
      }

      Schema contains = schema.getContains();
      if (contains != null) {
        int numberPassed = 0;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          validate(contains, document, PathUtils.append(uri, String.valueOf(idx)), errors::add,
              recursiveRef);
          if (errors.isEmpty()) {
            numberPassed++;
          }
        }
        Number minContains = schema.getMinContains();
        if (numberPassed < (minContains == null ? 1 : minContains.intValue())) {
          errorConsumer.accept(new MinContainsError(uri, document, schema));
        }
        Number maxContains = schema.getMaxContains();
        if (maxContains != null && numberPassed > maxContains.intValue()) {
          errorConsumer.accept(new MaxContainsError(uri, document, schema));
        }
      }
    } else if (object instanceof JSONObject) {
      typeCheck(schema, document, uri, setOf("object"), disallow, errorConsumer);
      JSONObject jsonObject = (JSONObject) object;
      Number maxProperties = schema.getMaxProperties();
      if (maxProperties != null && jsonObject.length() > maxProperties.intValue()) {
        errorConsumer.accept(new MaxPropertiesError(uri, document, schema));
      }
      Number minProperties = schema.getMinProperties();
      if (minProperties != null && jsonObject.length() < minProperties.intValue()) {
        errorConsumer.accept(new MinPropertiesError(uri, document, schema));
      }

      Collection<String> requiredProperties = schema.getRequiredProperties();
      for (String property : requiredProperties) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(new MissingPropertyError(uri, document, property, schema));
        }
      }

      Map<String, Schema> _properties = schema.getProperties();
      for (Map.Entry<String, Schema> entry : _properties.entrySet()) {
        if (!entry.getValue().isRequired()) {
          continue;
        }
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          errorConsumer.accept(new MissingPropertyError(uri, document, property, schema));
        }
      }

      Collection<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      Collection<Ecma262Pattern> patternPropertiesPatterns = schema.getPatternPropertiesPatterns();
      Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          validate(_properties.get(property), document, PathUtils.append(uri, property),
              errorConsumer, recursiveRef);
          remainingProperties.remove(property);
          selfPropertyHandler.accept(property);
        }

        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchema.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern1 = it0.next();
          Schema schema1 = it1.next();
          if (pattern1.matches(property)) {
            validate(
                schema1, document, PathUtils.append(uri, property), errorConsumer, recursiveRef);
            remainingProperties.remove(property);
            selfPropertyHandler.accept(property);
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
            validate(propertyNames, document, propertyPath, errorConsumer, recursiveRef);
          } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      Map<String, Schema> schemaDependencies = schema.getDependentSchemas();
      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }
        validate(entry.getValue(), document, uri, errorConsumer, selfPropertyHandler,
            selfItemHandler, recursiveRef);
      }

      Schema additionalProperties = schema.getAdditionalProperties();
      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          validate(additionalProperties, document, PathUtils.append(uri, property), errorConsumer,
              recursiveRef);
          selfPropertyHandler.accept(property);
        }
      }

      Schema unevaluatedProperties = schema.getUnevaluatedProperties();
      if (unevaluatedProperties != null) {
        Collection<String> remainingProperties2 = new HashSet<>(jsonObject.keySet());
        remainingProperties2.removeAll(evaluatedProperties);
        for (String property : remainingProperties2) {
          validate(unevaluatedProperties, document, PathUtils.append(uri, property), errorConsumer,
              recursiveRef);
          selfPropertyHandler.accept(property);
        }
      }

      Map<String, Collection<String>> dependencies = schema.getDependentRequired();
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
          errorConsumer.accept(new DependencyError(uri, document, property, dependency, schema));
        }
      }

    } else if (object == JSONObject.NULL) {
      typeCheck(schema, document, uri, setOf("null"), disallow, errorConsumer);
    } else if (object != null) {
      errorConsumer.accept(new UnexpecedTypeError(uri, document, object, schema));
    }

    Object _const = schema.getConst();
    if (_const != null) {
      if (!makeComparable(_const).equals(makeComparable(object))) {
        errorConsumer.accept(new ConstError(uri, document, schema));
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
        errorConsumer.accept(new EnumError(uri, document, schema));
      }
    }
  }

  private static void typeCheck(Schema schema, Object document, URI path, Set<String> types,
      Collection<String> disallow, Consumer<ValidationError> errorConsumer) {
    Collection<String> typesIn0 = new HashSet<>(types);
    typesIn0.retainAll(disallow);
    if (!typesIn0.isEmpty()) {
      errorConsumer.accept(new TypeDisallowedError(path, document, typesIn0, schema));
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

    errorConsumer.accept(new TypeError(path, document, explicitTypes, types, schema));
  }

  public static void validate(Schema schema, InputStream inputStream,
      Consumer<ValidationError> errorConsumer) throws IOException {
    validate(schema, loadJson(inputStream), errorConsumer);
  }

  public static void validate(Schema schema, File file) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, file, errors::add);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  public static void validate(Schema schema, URI uri) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, uri, errors::add);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  public static void validate(Schema schema, URL url) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, url, errors::add);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  public static void validate(Schema schema, Object document) throws ValidationException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, document, errors::add);
    if (!errors.isEmpty()) {
      throw new ValidationException(errors);
    }
  }

  public static void validate(Schema schema, InputStream inputStream)
      throws ValidationException, IOException {
    validate(schema, loadJson(inputStream));
  }

  public static void validate(Schema schema, URL url, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, loadJson(url.openStream()), errorConsumer);
  }

  public static void validate(Schema schema, URI uri, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, uri.toURL(), errorConsumer);
  }

  public static void validate(Schema schema, File file, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, file.toURI(), errorConsumer);
  }

  public static void validate(
      Schema schema, Object document, Consumer<ValidationError> errorConsumer) {
    validate(schema, document, URI.create(""), errorConsumer);
  }
}
