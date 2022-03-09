package net.jimblackler.jsonschemafriend;

import static java.util.Base64.getUrlDecoder;
import static net.jimblackler.jsonschemafriend.ComparableUtils.makeComparable;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_3;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_4;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_6;
import static net.jimblackler.jsonschemafriend.MetaSchemaUris.DRAFT_7;
import static net.jimblackler.jsonschemafriend.Utils.setOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class Validator {
  private static final Logger LOG = Logger.getLogger(Validator.class.getName());

  private final RegExPatternSupplier regExPatternSupplier;
  private final Predicate<? super ValidationError> errorFilter;

  private final boolean validateFormats;

  public Validator() {
    this(validationError -> true);
  }

  public Validator(boolean validateFormats) {
    this(new CachedRegExPatternSupplier(JoniRegExPattern::new), validationError -> true, validateFormats);
  }

  public Validator(Predicate<? super ValidationError> errorFilter) {
    this(new CachedRegExPatternSupplier(JoniRegExPattern::new), errorFilter);
  }

  public Validator(
      RegExPatternSupplier regExPatternSupplier, Predicate<? super ValidationError> errorFilter) {
   this(regExPatternSupplier, errorFilter, false);
  }

  public Validator(
      RegExPatternSupplier regExPatternSupplier, Predicate<? super ValidationError> errorFilter, boolean validateFormats) {
    this.regExPatternSupplier = regExPatternSupplier;
    this.errorFilter = errorFilter;
    this.validateFormats = validateFormats;
  }

  public static Object getObject(Object document, URI uri) throws MissingPathException {
    Object object;
    String query = uri.getQuery();
    if (query == null) {
      object = PathUtils.fetchFromPath(document, uri.getRawFragment());
    } else {
      // Query part can carry a string for validation while preserving the rest of the URI for error
      // messages. This is used for propertyName validation where it's not possible to link to the
      // name with a standard JSON Pointer.
      object = query;
    }
    return object;
  }

  public void validate(
      Schema schema, Object document, URI uri, Consumer<ValidationError> errorConsumer) {
    validate(schema, document, uri, errorConsumer, property -> {}, item -> {}, new HashMap<>());
  }

  public void validate(Schema schema, Object document, URI uri,
      Consumer<ValidationError> errorConsumer, Map<String, Schema> dynamicAnchors) {
    validate(schema, document, uri, errorConsumer, property -> {}, item -> {}, dynamicAnchors);
  }

  public void validate(Schema schema, Object document, URI uri,
      Consumer<ValidationError> errorConsumer, Consumer<String> propertyConsumer,
      Consumer<Integer> itemConsumer, Map<String, Schema> dynamicAnchorsIn) {
    Object object;
    try {
      object = getObject(document, uri);
    } catch (MissingPathException e) {
      throw new IllegalStateException(e);
    }

    Consumer<ValidationError> error = validationError -> {
      if (errorFilter.test(validationError)) {
        errorConsumer.accept(validationError);
      }
    };

    if (schema.isFalse()) {
      error.accept(new FalseSchemaError(uri, document, schema));
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

    Map<String, Schema> dynamicAnchors = new HashMap<>(dynamicAnchorsIn);
    for (Map.Entry<String, Schema> entry : schema.getDynamicAnchorsInResource().entrySet()) {
      String anchor = entry.getKey();
      // We don't overwrite existing anchors, because "A $dynamicRef should resolve to the *first*
      // $dynamicAnchor still in scope that is encountered when the schema is evaluated."
      if (dynamicAnchors.containsKey(anchor)) {
        continue;
      }
      dynamicAnchors.put(anchor, entry.getValue());
    }

    // To reduce parameter proliferation the same dynamicAnchors map is used for both 2019-09
    // and 2020-12 validate-time anchors recursiveAnchors are stored as dynamicAnchors with key
    // 'null'.
    if (schema.isRecursiveAnchor() && !dynamicAnchors.containsKey(null)) {
      dynamicAnchors.put(null, schema);
    }

    Schema _if = schema.getIf();
    Schema _then = schema.getThen();
    Schema _else = schema.getElse();

    if (_if != null) {
      List<ValidationError> errors = new ArrayList<>();
      Collection<String> unevaluatedProperties = new HashSet<>();
      Collection<Integer> unevaluatedItems = new HashSet<>();
      validate(_if, document, uri, errors::add, unevaluatedProperties::add, unevaluatedItems::add,
          dynamicAnchors);
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
            dynamicAnchors);
      }
    }

    Schema ref = schema.getRef();
    if (ref != null) {
      validate(
          ref, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler, dynamicAnchors);
    }

    Schema recursiveRef1 = schema.getRecursiveRef();
    if (recursiveRef1 != null) {
      validate(recursiveRef1.isRecursiveAnchor() ? dynamicAnchors.get(null) : recursiveRef1,
          document, uri, errorConsumer, selfPropertyHandler, selfItemHandler, dynamicAnchors);
    }

    URI dynamicRefURI = schema.getDynamicRefURI();
    if (dynamicRefURI != null) {
      String anchor = dynamicRefURI.getFragment();
      // "A $dynamicRef without a matching $dynamicAnchor in the same schema resource should behave
      // like a normal $ref to $anchor."
      Schema toValidate = schema.getDynamicAnchorsInResource().containsKey(anchor)
          ? dynamicAnchors.get(anchor)
          : schema.getDefaultDynamicRef();
      // "A $dynamicRef that initially resolves to a schema with a matching $dynamicAnchor should
      // resolve to the first $dynamicAnchor in the dynamic scope."
      if (anchor.equals(toValidate.getDynamicAnchor())) {
        toValidate = dynamicAnchors.get(anchor);
      }
      if (toValidate == null) {
        LOG.warning("Could not resolve dynamic anchor: " + anchor);
      } else {
        validate(toValidate, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler,
            dynamicAnchors);
      }
    }

    Collection<Schema> allOf = schema.getAllOf();
    for (Schema schema1 : allOf) {
      validate(schema1, document, uri, errorConsumer, selfPropertyHandler, selfItemHandler,
          dynamicAnchors);
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
            unevaluatedItems::add, dynamicAnchors);
        if (errors.isEmpty()) {
          numberPassed++;
          unevaluatedProperties.forEach(selfPropertyHandler);
          unevaluatedItems.forEach(selfItemHandler);
        }
        allErrors.add(errors);
      }
      if (numberPassed == 0) {
        error.accept(new AnyOfError(uri, document, allErrors, schema));
      }
    }

    Collection<Schema> oneOf = schema.getOneOf();
    if (oneOf != null) {
      List<Schema> passed = new ArrayList<>();
      List<List<ValidationError>> allErrors = new ArrayList<>();
      for (Schema schema1 : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        List<String> unevaluatedProperties = new ArrayList<>();
        List<Integer> unevaluatedItems = new ArrayList<>();
        validate(schema1, document, uri, errors::add, unevaluatedProperties::add,
            unevaluatedItems::add, dynamicAnchors);
        if (errors.isEmpty()) {
          passed.add(schema1);
          unevaluatedProperties.forEach(selfPropertyHandler);
          unevaluatedItems.forEach(selfItemHandler);
        }
        allErrors.add(errors);
      }
      if (passed.size() != 1) {
        error.accept(new OneOfError(uri, document, passed, allErrors, schema));
      }
    }

    Schema not = schema.getNot();
    if (not != null) {
      List<ValidationError> errors = new ArrayList<>();
      validate(not, document, uri, errors::add, dynamicAnchors);
      if (errors.isEmpty()) {
        error.accept(new NotError(uri, document, schema));
      }
    }

    Collection<Schema> disallowSchemas = schema.getDisallowSchemas();
    for (Schema disallowSchema : disallowSchemas) {
      List<ValidationError> errors = new ArrayList<>();
      validate(disallowSchema, document, uri, errors::add, selfPropertyHandler, selfItemHandler,
          dynamicAnchors);
      if (errors.isEmpty()) {
        error.accept(new DisallowError(uri, document, schema));
      }
    }

    Number multipleOf = schema.getMultipleOf();
    Number minimum = schema.getMinimum();
    Number maximum = schema.getMaximum();
    Number exclusiveMinimum = schema.getExclusiveMinimum();
    Number exclusiveMaximum = schema.getExclusiveMaximum();
    boolean exclusiveMinimumBoolean = schema.isExclusiveMinimumBoolean();
    boolean exclusiveMaximumBoolean = schema.isExclusiveMaximumBoolean();
    Collection<String> disallow = schema.getDisallow();

    if (object instanceof Number) {
      Number number = (Number) object;
      if (multipleOf != null
          && (new BigDecimal(number.toString())
                  .remainder(new BigDecimal(multipleOf.toString()))
                  .compareTo(BigDecimal.ZERO)
              != 0)) {
        error.accept(new MultipleError(uri, document, schema));
      }
      if (maximum != null
          && (exclusiveMaximumBoolean ? number.doubleValue() >= maximum.doubleValue()
                                      : number.doubleValue() > maximum.doubleValue())) {
        error.accept(new MaximumError(uri, document, schema));
      }

      if (exclusiveMaximum != null && number.doubleValue() >= exclusiveMaximum.doubleValue()) {
        error.accept(new ExclusiveMaximumError(uri, document, schema));
      }
      if (minimum != null) {
        if (exclusiveMinimumBoolean ? number.doubleValue() <= minimum.doubleValue()
                                    : number.doubleValue() < minimum.doubleValue()) {
          error.accept(new MinimumError(uri, document, schema));
        }
      }
      if (exclusiveMinimum != null && number.doubleValue() <= exclusiveMinimum.doubleValue()) {
        error.accept(new ExclusiveMinimumError(uri, document, schema));
      }
      Set<String> okTypes = new HashSet<>();
      okTypes.add("number");
      try {
        boolean preDraft5 =
            DRAFT_3.equals(schema.getMetaSchema()) || DRAFT_4.equals(schema.getMetaSchema());
        if (preDraft5) {
          if (!(number instanceof Float) && !(number instanceof Double)) {
            okTypes.add("integer");
          }
        } else {
          BigDecimal bigDecimal = new BigDecimal(number.toString());
          if (bigDecimal.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            okTypes.add("integer");
          }
        }
      } catch (NumberFormatException e) {
        // Intentionally silenced.
      }

      typeCheck(schema, document, uri, okTypes, disallow, error);

      Number divisibleBy = schema.getDivisibleBy();
      if (divisibleBy != null && number.doubleValue() / divisibleBy.doubleValue() % 1 != 0) {
        error.accept(new DivisibleByError(uri, document, schema));
      }

    } else if (object instanceof String) {
      String string = (String) object;
      int unicodeCompliantLength = string.codePointCount(0, string.length());
      Number minLength = schema.getMinLength();
      Number maxLength = schema.getMaxLength();
      if (maxLength != null && unicodeCompliantLength > maxLength.intValue()) {
        error.accept(new MaxLengthError(uri, document, schema));
      }
      if (minLength != null && unicodeCompliantLength < minLength.intValue()) {
        error.accept(new MinLengthError(uri, document, schema));
      }
      String patternString = schema.getPattern();
      if (patternString != null) {
        try {
          if (!regExPatternSupplier.newPattern(patternString).matches(string)) {
            error.accept(new PatternError(uri, document, schema));
          }
        } catch (InvalidRegexException e) {
          LOG.warning("Invalid regex " + patternString);
        }
      }

      String format = schema.getFormat();
      if (format != null) {
        String message =
            FormatChecker.formatCheck(string, format, schema.getMetaSchema(), regExPatternSupplier, this.validateFormats);
        if (message != null) {
          error.accept(new FormatError(uri, document, schema, message));
        }
      }
      String stringToValidate = string;
      String contentEncoding = schema.getContentEncoding();

      boolean preDraft5 =
          DRAFT_3.equals(schema.getMetaSchema()) || DRAFT_4.equals(schema.getMetaSchema());
      boolean preDraft2019 = preDraft5 || DRAFT_6.equals(schema.getMetaSchema())
          || DRAFT_7.equals(schema.getMetaSchema());
      if (preDraft2019) {
        if ("base64".equals(contentEncoding)) {
          Base64.Decoder urlDecoder = getUrlDecoder();
          byte[] decoded = null;
          try {
            decoded = urlDecoder.decode(string);
          } catch (IllegalArgumentException e) {
            error.accept(new ContentEncodingError(uri, document, schema, e.getMessage()));
          }
          if (decoded != null) {
            stringToValidate = new String(decoded, StandardCharsets.UTF_8);
          }
        }

        String contentMediaType = schema.getContentMediaType();
        if ("application/json".equals(contentMediaType)) {
          try {
            new ObjectMapper().readValue(stringToValidate, Object.class);
          } catch (JsonProcessingException e) {
            error.accept(new ContentEncodingError(uri, document, schema, e.getMessage()));
          }
        }
      }

      typeCheck(schema, document, uri, setOf("string"), disallow, errorConsumer);
    } else if (object instanceof Boolean) {
      typeCheck(schema, document, uri, setOf("boolean"), disallow, errorConsumer);
    } else if (object instanceof List) {
      typeCheck(schema, document, uri, setOf("array"), disallow, errorConsumer);
      Collection<Object> jsonArray = (Collection<Object>) object;
      List<Schema> prefixItems = schema.getPrefixItems();
      int itemStart = 0;
      if (prefixItems != null) {
        itemStart = prefixItems.size();
        for (int idx = 0; idx != Math.min(prefixItems.size(), jsonArray.size()); idx++) {
          validate(prefixItems.get(idx), document, PathUtils.append(uri, String.valueOf(idx)),
              errorConsumer, dynamicAnchors);
          selfItemHandler.accept(idx);
        }
      } else {
        List<Schema> itemsTuple = schema.getItemsTuple();
        if (itemsTuple != null) {
          Schema additionalItems = schema.getAdditionalItems();
          if (jsonArray.size() > itemsTuple.size() && additionalItems != null) {
            for (int idx = itemsTuple.size(); idx != jsonArray.size(); idx++) {
              validate(additionalItems, document, PathUtils.append(uri, String.valueOf(idx)),
                  errorConsumer, dynamicAnchors);
              selfItemHandler.accept(idx);
            }
          }
          for (int idx = 0; idx != Math.min(itemsTuple.size(), jsonArray.size()); idx++) {
            validate(itemsTuple.get(idx), document, PathUtils.append(uri, String.valueOf(idx)),
                errorConsumer, dynamicAnchors);
            selfItemHandler.accept(idx);
          }
        }
      }

      Schema _items = schema.getItems();
      if (_items != null) {
        for (int idx = itemStart; idx < jsonArray.size(); idx++) {
          validate(_items, document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer,
              dynamicAnchors);
          selfItemHandler.accept(idx);
        }
      }
      Schema contains = schema.getContains();
      if (contains != null) {
        int numberPassed = 0;
        for (int idx = 0; idx != jsonArray.size(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          validate(contains, document, PathUtils.append(uri, String.valueOf(idx)), errors::add,
              dynamicAnchors);
          if (errors.isEmpty()) {
            selfItemHandler.accept(idx);
            numberPassed++;
          }
        }
        Number minContains = schema.getMinContains();
        if (numberPassed < (minContains == null ? 1 : minContains.intValue())) {
          error.accept(new MinContainsError(uri, document, schema));
        }
        Number maxContains = schema.getMaxContains();
        if (maxContains != null && numberPassed > maxContains.intValue()) {
          error.accept(new MaxContainsError(uri, document, schema));
        }
      }

      Schema unevaluatedItems = schema.getUnevaluatedItems();
      if (unevaluatedItems != null) {
        for (int idx = 0; idx != jsonArray.size(); idx++) {
          if (evaluatedItems.contains(idx)) {
            continue;
          }
          validate(unevaluatedItems, document, PathUtils.append(uri, String.valueOf(idx)),
              errorConsumer, dynamicAnchors);
          selfItemHandler.accept(idx);
        }
      }

      Number maxItems = schema.getMaxItems();
      if (maxItems != null && jsonArray.size() > maxItems.intValue()) {
        error.accept(new MaxItemsError(uri, document, schema));
      }

      Number minItems = schema.getMinItems();
      if (minItems != null && jsonArray.size() < minItems.intValue()) {
        error.accept(new MinItemsError(uri, document, schema));
      }

      if (schema.isUniqueItems()) {
        Collection<Object> items = new HashSet<>();
        for (Object o : jsonArray) {
          if (!items.add(makeComparable(o))) {
            error.accept(new UniqueItemsError(uri, document, schema));
          }
        }
      }

    } else if (object instanceof Map) {
      typeCheck(schema, document, uri, setOf("object"), disallow, errorConsumer);
      Map<String, Object> jsonObject = (Map<String, Object>) object;
      Number maxProperties = schema.getMaxProperties();
      if (maxProperties != null && jsonObject.size() > maxProperties.intValue()) {
        error.accept(new MaxPropertiesError(uri, document, schema));
      }
      Number minProperties = schema.getMinProperties();
      if (minProperties != null && jsonObject.size() < minProperties.intValue()) {
        error.accept(new MinPropertiesError(uri, document, schema));
      }

      Collection<String> requiredProperties = schema.getRequiredProperties();
      for (String property : requiredProperties) {
        if (!jsonObject.containsKey(property)) {
          error.accept(new MissingPropertyError(uri, document, property, schema));
        }
      }

      Map<String, Schema> _properties = schema.getProperties();
      for (Map.Entry<String, Schema> entry : _properties.entrySet()) {
        if (!entry.getValue().isRequired()) {
          continue;
        }
        String property = entry.getKey();
        if (!jsonObject.containsKey(property)) {
          error.accept(new MissingPropertyError(uri, document, property, schema));
        }
      }

      Collection<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      Collection<String> patternPropertiesPatterns = schema.getPatternPropertiesPatterns();
      Collection<Schema> patternPropertiesSchema = schema.getPatternPropertiesSchema();
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          validate(_properties.get(property), document, PathUtils.append(uri, property),
              errorConsumer, dynamicAnchors);
          remainingProperties.remove(property);
          selfPropertyHandler.accept(property);
        }

        Iterator<String> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchema.iterator();
        while (it0.hasNext()) {
          String pattern1 = it0.next();
          Schema schema1 = it1.next();
          try {
            if (regExPatternSupplier.newPattern(pattern1).matches(property)) {
              validate(schema1, document, PathUtils.append(uri, property), errorConsumer,
                  dynamicAnchors);
              remainingProperties.remove(property);
              selfPropertyHandler.accept(property);
            }
          } catch (InvalidRegexException e) {
            LOG.warning("Invalid regex: " + e.getMessage());
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
            validate(propertyNames, document, propertyPath, errorConsumer, dynamicAnchors);
          } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      Map<String, Schema> schemaDependencies = schema.getDependentSchemas();
      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.containsKey(property)) {
          continue;
        }
        validate(entry.getValue(), document, uri, errorConsumer, selfPropertyHandler,
            selfItemHandler, dynamicAnchors);
      }

      Schema additionalProperties = schema.getAdditionalProperties();
      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          validate(additionalProperties, document, PathUtils.append(uri, property), errorConsumer,
              dynamicAnchors);
          selfPropertyHandler.accept(property);
        }
      }

      Schema unevaluatedProperties = schema.getUnevaluatedProperties();
      if (unevaluatedProperties != null) {
        Collection<String> remainingProperties2 = new HashSet<>(jsonObject.keySet());
        remainingProperties2.removeAll(evaluatedProperties);
        for (String property : remainingProperties2) {
          validate(unevaluatedProperties, document, PathUtils.append(uri, property), errorConsumer,
              dynamicAnchors);
          selfPropertyHandler.accept(property);
        }
      }

      Map<String, Collection<String>> dependencies = schema.getDependentRequired();
      for (Map.Entry<String, Collection<String>> entry : dependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.containsKey(property)) {
          continue;
        }

        Collection<String> _dependencies = entry.getValue();
        for (String dependency : _dependencies) {
          if (jsonObject.containsKey(dependency)) {
            continue;
          }
          error.accept(new DependencyError(uri, document, property, dependency, schema));
        }
      }

    } else if (object == null) {
      typeCheck(schema, document, uri, setOf("null"), disallow, errorConsumer);
    } else {
      error.accept(new UnexpectedTypeError(uri, document, object, schema));
    }

    if (schema.hasConst()) {
      if (!makeComparable(schema.getConst()).equals(makeComparable(object))) {
        error.accept(new ConstError(uri, document, schema));
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
        error.accept(new EnumError(uri, document, schema));
      }
    }
  }

  private void typeCheck(Schema schema, Object document, URI path, Set<String> types,
      Collection<String> disallow, Consumer<? super ValidationError> errorConsumer) {
    if (!disallow.isEmpty()) {
      Collection<String> typesIn0 = new HashSet<>(types);
      typesIn0.retainAll(disallow);
      if (!typesIn0.isEmpty()) {
        errorConsumer.accept(new TypeDisallowedError(path, document, typesIn0, schema));
      }
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

    Collection<String> typesIn;
    if (explicitTypes.isEmpty() && typesSchema.isEmpty()) {
      typesIn = types;
    } else {
      typesIn = new HashSet<>(types);
      typesIn.retainAll(explicitTypes);
    }
    if (!typesIn.isEmpty()) {
      return;
    }

    errorConsumer.accept(new TypeError(path, document, explicitTypes, types, schema));
  }

  public void validate(Schema schema, File file) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, file, errors::add);
    if (!errors.isEmpty()) {
      throw new ListValidationException(errors);
    }
  }

  public void validate(Schema schema, URI uri) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, uri, errors::add);
    if (!errors.isEmpty()) {
      throw new ListValidationException(errors);
    }
  }

  public void validate(Schema schema, URL url) throws ValidationException, IOException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, url, errors::add);
    if (!errors.isEmpty()) {
      throw new ListValidationException(errors);
    }
  }

  public void validate(Schema schema, Object document, URI uri) throws ValidationException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, document, uri, errors::add);
    if (!errors.isEmpty()) {
      throw new ListValidationException(errors);
    }
  }

  public void validate(Schema schema, Object document) throws ValidationException {
    Collection<ValidationError> errors = new ArrayList<>();
    validate(schema, document, errors::add);
    if (!errors.isEmpty()) {
      throw new ListValidationException(errors);
    }
  }

  public void validateJson(Schema schema, String string) throws ValidationException {
    try {
      validate(schema, new ObjectMapper().readValue(string, Object.class));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public void validate(Schema schema, InputStream inputStream)
      throws ValidationException, IOException {
    validate(schema, new ObjectMapper().readValue(inputStream, Object.class));
  }

  public void validate(Schema schema, URL url, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, new ObjectMapper().readValue(url.openStream(), Object.class), errorConsumer);
  }

  public void validate(Schema schema, URI uri, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, uri.toURL(), errorConsumer);
  }

  public void validate(Schema schema, File file, Consumer<ValidationError> errorConsumer)
      throws IOException {
    validate(schema, file.toURI(), errorConsumer);
  }

  public void validate(Schema schema, Object document, Consumer<ValidationError> errorConsumer) {
    validate(schema, document, URI.create(""), errorConsumer);
  }

  public Map<String, Object> validateWithOutput(Schema schema, Object document)
      throws GenerationException {
    Map<String, Object> output = new LinkedHashMap<>();
    output.put("valid", true);
    output.put("keywordLocation", schema.getUri().toString());
    output.put("absoluteKeywordLocation", schema.getResourceUri().toString());
    output.put("instanceLocation", "");
    validate(schema, document, validationError -> {
      output.put("valid", false);
      Map<String, Object> error = new LinkedHashMap<>();
      error.put("valid", false);
      error.put("error", validationError.getMessage());
      Schema failedSubSchema = validationError.getSchema();
      error.put("keywordLocation", failedSubSchema.getUri().toString());
      error.put("absoluteKeywordLocation", failedSubSchema.getResourceUri().toString());
      String rawFragment = validationError.getUri().getRawFragment();
      error.put("instanceLocation", "#" + (rawFragment == null ? "" : rawFragment));
      if (!output.containsKey("errors")) {
        output.put("errors", new ArrayList<>());
      }
      ((Collection<Object>) output.get("errors")).add(error);
    });
    return output;
  }
}
