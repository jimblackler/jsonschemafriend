package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.ComparableMutable.makeComparable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

/**
 * A schema defined by an object. "Object" refers to the type in the definition, not the type of
 * data it validates.
 */
public class ObjectSchema extends Schema {
  private final JSONObject schemaJson; // Kept for debugging only.

  private final URI metaSchemaUri;

  // number checks
  private final Number multipleOf;
  private final Number maximum;
  private final Object exclusiveMaximum;
  private final Number minimum;
  private final Object exclusiveMinimum;
  private final Number divisibleBy;

  // string checks
  private final Number maxLength;
  private final Number minLength;
  private final Ecma262Pattern pattern;

  // array checks
  private final Schema additionalItems;
  private final Schema _items;
  private final List<Schema> itemsArray;
  private final Number maxItems;
  private final Number minItems;
  private final boolean uniqueItems;
  private final Schema contains;

  // object checks
  private final Number maxProperties;
  private final Number minProperties;
  private final Collection<String> requiredProperties = new HashSet<>();
  private final Schema additionalProperties;
  private final Map<String, Schema> _properties = new HashMap<>();
  private final Collection<Ecma262Pattern> patternPropertiesPatterns = new ArrayList<>();
  private final Collection<Schema> patternPropertiesSchemas = new ArrayList<>();
  private final Map<String, Collection<String>> dependencies = new HashMap<>();
  private final Map<String, Schema> schemaDependencies = new HashMap<>();
  private final Schema propertyNames;

  // all types checks
  private final Object _const;
  private final List<Object> enums;
  private final Set<String> explicitTypes;
  private final Set<String> inferredTypes = new HashSet<>();
  private final Collection<Schema> typesSchema = new HashSet<>();
  private final Schema _if;
  private final Schema _then;
  private final Schema _else;
  private final Collection<Schema> allOf = new ArrayList<>();
  private final Collection<Schema> anyOf;
  private final Collection<Schema> oneOf;
  private final Schema not;

  private final Set<String> disallow = new HashSet<>();
  private final Collection<Schema> disallowSchemas = new HashSet<>();
  private final Object defaultValue;

  private boolean fullyBuilt = false;

  public ObjectSchema(SchemaStore schemaStore, URI uri, URI defaultMetaSchema)
      throws GenerationException {
    super(schemaStore, uri);

    URI baseDocumentUri = PathUtils.baseDocumentFromUri(uri);
    JSONObject baseDocument =
        (JSONObject) schemaStore.getDocumentSource().fetchDocument(baseDocumentUri);
    JSONObject jsonObjectOriginal =
        (JSONObject) PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());

    if (baseDocument == jsonObjectOriginal && baseDocument.has("$schema")) {
      metaSchemaUri = URI.create(baseDocument.getString("$schema"));
    } else {
      metaSchemaUri = defaultMetaSchema;
    }

    // It would be more convenient to work with a fully-built Schema from the meta-schema, not just
    // its JSON representation. However that isn't possible when building a self-referencing schema
    // (all JSON schema meta-schemas as self-referencing).
    JSONObject metaSchemaDocument =
        (JSONObject) schemaStore.getDocumentSource().fetchDocument(metaSchemaUri);

    schemaJson = jsonObjectOriginal; // Retained for debugging convenience only.

    // Create a new version of the object with only the properties that are explicitly in the
    // metaschema. This means that features from other version of the metaschema from working when
    // they shouldn't.
    JSONObject jsonObject = new JSONObject();
    {
      JSONObject properties = metaSchemaDocument.optJSONObject("properties");
      if (properties != null) {
        for (String property : properties.keySet()) {
          if (jsonObjectOriginal.has(property)) {
            jsonObject.put(property, jsonObjectOriginal.get(property));
          }
        }
      }
    }

    // number checks
    multipleOf = (Number) jsonObject.opt("multipleOf");
    maximum = (Number) jsonObject.opt("maximum");
    exclusiveMaximum = jsonObject.opt("exclusiveMaximum");
    minimum = (Number) jsonObject.opt("minimum");
    exclusiveMinimum = jsonObject.opt("exclusiveMinimum");
    divisibleBy = (Number) jsonObject.opt("divisibleBy");
    if (jsonObject.has("multipleOf") || jsonObject.has("maximum")
        || jsonObject.has("exclusiveMaximum") || jsonObject.has("minimum")
        || jsonObject.has("exclusiveMinimum") || jsonObject.has("multdivisibleBy")) {
      inferredTypes.add("number");
    }

    // string checks
    maxLength = (Number) jsonObject.opt("maxLength");
    minLength = (Number) jsonObject.opt("minLength");
    Object patternObject = jsonObject.opt("pattern");
    if (patternObject == null) {
      pattern = null;
    } else {
      pattern = new Ecma262Pattern((String) patternObject);
    }
    if (jsonObject.has("maxLength") || jsonObject.has("minLength") || jsonObject.has("pattern")) {
      inferredTypes.add("string");
    }

    // array checks
    additionalItems = getSubSchema(jsonObject, "additionalItems", uri);
    _items = getSubSchema(jsonObject, "items", uri);

    Object itemsObject = jsonObject.opt("items");
    URI itemsPath = PathUtils.append(uri, "items");
    if (itemsObject instanceof JSONArray) {
      itemsArray = new ArrayList<>();
      JSONArray jsonArray = (JSONArray) itemsObject;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        itemsArray.add(getSubSchema(PathUtils.append(itemsPath, String.valueOf(idx))));
      }
    } else {
      itemsArray = null;
    }

    maxItems = (Number) jsonObject.opt("maxItems");
    minItems = (Number) jsonObject.opt("minItems");
    uniqueItems = jsonObject.optBoolean("uniqueItems", false);
    contains = getSubSchema(jsonObject, "contains", uri);

    if (jsonObject.has("additionalItems") || jsonObject.has("items") || jsonObject.has("maxItems")
        || jsonObject.has("minItems") || jsonObject.has("uniqueItems")
        || jsonObject.has("contains")) {
      inferredTypes.add("array");
    }

    // object checks
    maxProperties = (Number) jsonObject.opt("maxProperties");
    minProperties = (Number) jsonObject.opt("minProperties");

    Object requiredObject = jsonObject.opt("required");
    if (requiredObject instanceof JSONArray) {
      JSONArray array = (JSONArray) requiredObject;
      for (int idx = 0; idx != array.length(); idx++) {
        requiredProperties.add(array.getString(idx));
      }
    }

    additionalProperties = getSubSchema(jsonObject, "additionalProperties", uri);

    Object propertiesObject = jsonObject.opt("properties");
    if (propertiesObject instanceof JSONObject) {
      JSONObject properties = (JSONObject) propertiesObject;
      URI propertiesPointer = PathUtils.append(uri, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        URI propertyUri = PathUtils.append(propertiesPointer, propertyName);

        // This is the only time that a schema conditions are directly changed by something in a
        // child schema (presence of 'required=true'). It requires this unorthodox process to look
        // inside the child schema ahead of the normal time (the child schema itself ignores the
        // 'required=true'). It's probably why this method of specifying required properties was
        // removed from Draft 4 onwards. It's only here for legacy support.
        Object propertyObject =
            new JSONPointer("#" + propertyUri.getRawFragment()).queryFrom(baseDocument);
        if (propertyObject instanceof JSONObject) {
          JSONObject propertyJsonObject = (JSONObject) propertyObject;
          if (propertyJsonObject.optBoolean("required")) {
            requiredProperties.add(propertyName);
          }
        }
        _properties.put(propertyName, getSubSchema(propertyUri));
      }
    }

    Object patternPropertiesObject = jsonObject.opt("patternProperties");
    if (patternPropertiesObject instanceof JSONObject) {
      JSONObject patternProperties = (JSONObject) patternPropertiesObject;
      URI propertiesPointer = PathUtils.append(uri, "patternProperties");
      Iterator<String> it = patternProperties.keys();
      while (it.hasNext()) {
        String propertyPattern = it.next();
        patternPropertiesPatterns.add(new Ecma262Pattern(propertyPattern));
        URI patternPointer = PathUtils.append(propertiesPointer, propertyPattern);
        patternPropertiesSchemas.add(getSubSchema(patternPointer));
      }
    }

    JSONObject dependenciesJsonObject = jsonObject.optJSONObject("dependencies");
    if (dependenciesJsonObject != null) {
      for (String dependency : dependenciesJsonObject.keySet()) {
        Collection<String> spec = new ArrayList<>();
        Object dependencyObject = dependenciesJsonObject.get(dependency);
        if (dependencyObject instanceof JSONArray) {
          JSONArray array = (JSONArray) dependencyObject;
          for (int idx = 0; idx != array.length(); idx++) {
            spec.add(array.getString(idx));
          }
          dependencies.put(dependency, spec);
        } else if (dependencyObject instanceof JSONObject || dependencyObject instanceof Boolean) {
          URI dependenciesPpinter = PathUtils.append(uri, "dependencies");
          schemaDependencies.put(
              dependency, getSubSchema(PathUtils.append(dependenciesPpinter, dependency)));
        } else {
          Collection<String> objects = new ArrayList<>();
          objects.add((String) dependencyObject);
          dependencies.put(dependency, objects);
        }
      }
    }

    propertyNames = getSubSchema(jsonObject, "propertyNames", uri);

    if (jsonObject.has("maxProperties") || jsonObject.has("minProperties")
        || jsonObject.has("required") || jsonObject.has("additionalProperties")
        || jsonObject.has("properties") || jsonObject.has("patternProperties")
        || jsonObject.has("dependencies") || jsonObject.has("propertyNames")) {
      inferredTypes.add("object");
    }

    // all types checks
    _const = jsonObject.opt("const");

    JSONArray enumArray = jsonObject.optJSONArray("enum");
    if (enumArray == null) {
      enums = null;
    } else {
      enums = new ArrayList<>();
      for (int idx = 0; idx != enumArray.length(); idx++) {
        Object enumObject = enumArray.get(idx);
        enums.add(enumObject);
        inferredTypes.add(javaToSchemaType(enumObject));
      }
    }

    Object typeObject = jsonObject.opt("type");
    if (typeObject instanceof JSONArray) {
      URI typePointer = PathUtils.append(uri, "type");
      explicitTypes = new HashSet<>();
      JSONArray array = (JSONArray) typeObject;
      for (int idx = 0; idx != array.length(); idx++) {
        Object arrayEntryObject = array.get(idx);
        if (arrayEntryObject instanceof Boolean || arrayEntryObject instanceof JSONObject) {
          typesSchema.add(getSubSchema(PathUtils.append(typePointer, String.valueOf(idx))));
        } else {
          explicitTypes.add((String) arrayEntryObject);
        }
      }
    } else if (typeObject instanceof String) {
      explicitTypes = setOf(typeObject.toString());
    } else {
      explicitTypes = null;
    }

    _if = getSubSchema(jsonObject, "if", uri);
    _then = getSubSchema(jsonObject, "then", uri);
    _else = getSubSchema(jsonObject, "else", uri);

    Object allOfObject = jsonObject.opt("allOf");
    if (allOfObject instanceof JSONArray) {
      JSONArray array = (JSONArray) allOfObject;
      URI arrayPath = PathUtils.append(uri, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = PathUtils.append(arrayPath, String.valueOf(idx));
        allOf.add(getSubSchema(indexPointer));
      }
    }

    Object extendsObject = jsonObject.opt("extends");
    if (extendsObject instanceof JSONArray) {
      URI arrayPath = PathUtils.append(uri, "extends");
      JSONArray array = (JSONArray) extendsObject;
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = PathUtils.append(arrayPath, String.valueOf(idx));
        allOf.add(getSubSchema(indexPointer));
      }
    } else if (extendsObject instanceof JSONObject || extendsObject instanceof Boolean) {
      URI arrayPath = PathUtils.append(uri, "extends");
      allOf.add(getSubSchema(arrayPath));
    }

    Object anyOfObject = jsonObject.opt("anyOf");
    if (anyOfObject instanceof JSONArray) {
      anyOf = new ArrayList<>();
      JSONArray array = (JSONArray) anyOfObject;
      URI arrayPath = PathUtils.append(uri, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = PathUtils.append(arrayPath, String.valueOf(idx));
        anyOf.add(getSubSchema(indexPointer));
      }
    } else {
      anyOf = null;
    }

    Object oneOfObject = jsonObject.opt("oneOf");
    if (oneOfObject instanceof JSONArray) {
      oneOf = new ArrayList<>();
      JSONArray array = (JSONArray) oneOfObject;
      URI arrayPath = PathUtils.append(uri, "oneOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = PathUtils.append(arrayPath, String.valueOf(idx));
        oneOf.add(getSubSchema(indexPointer));
      }
    } else {
      oneOf = null;
    }

    not = getSubSchema(jsonObject, "not", uri);

    Object disallowObject = jsonObject.opt("disallow");
    if (disallowObject instanceof String) {
      disallow.add(disallowObject.toString());
    } else if (disallowObject instanceof JSONArray) {
      JSONArray array = (JSONArray) disallowObject;
      URI disallowPointer = PathUtils.append(uri, "disallow");
      for (int idx = 0; idx != array.length(); idx++) {
        Object disallowEntryObject = array.get(idx);
        if (disallowEntryObject instanceof String) {
          disallow.add(array.getString(idx));
        } else {
          disallowSchemas.add(getSubSchema(PathUtils.append(disallowPointer, String.valueOf(idx))));
        }
      }
    }

    defaultValue = jsonObject.opt("default");
    if (defaultValue != null) {
      inferredTypes.add(javaToSchemaType(defaultValue));
    }
    fullyBuilt = true;
  }

  private static String javaToSchemaType(Object object) {
    if (object.getClass().isArray()) {
      return "array";
    }
    if (object instanceof Integer) {
      return "integer";
    }

    if (object instanceof Long) {
      return "integer";
    }

    if (object instanceof Short) {
      return "integer";
    }

    if (object instanceof Byte) {
      return "integer";
    }

    if (object instanceof Number) {
      return "number";
    }

    if (object instanceof String) {
      return "string";
    }

    if (object instanceof Boolean) {
      return "boolean";
    }

    return "object";
  }

  private static Set<String> setOf(String string) {
    // Set.of() is only available in Java 9+. We try to keep the libary as compatible as possible.
    Set<String> set = new HashSet<>();
    set.add(string);
    return set;
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

  public Number getMultipleOf() {
    return multipleOf;
  }

  public Number getMaximum() {
    return maximum;
  }

  public Number getMinimum() {
    return minimum;
  }

  private Schema getSubSchema(JSONObject jsonObject, String name, URI uri)
      throws GenerationException {
    Object object = jsonObject.opt(name);
    if (object instanceof JSONObject || object instanceof Boolean) {
      return getSubSchema(PathUtils.append(uri, name));
    }
    return null;
  }

  private Schema getSubSchema(URI uri) throws GenerationException {
    Schema subSchema = schemaStore.getSchema(uri, metaSchemaUri);
    if (subSchema != null && subSchema.getUri().equals(uri)) {
      subSchema.setParent(this);
    }
    return subSchema;
  }

  @Override
  public void validate(Object document, URI uri, Consumer<ValidationError> errorConsumer) {
    Object object;
    String query = uri.getQuery();
    if (query == null || query.isEmpty()) {
      object = PathUtils.fetchFromPath(document, uri.getRawFragment());
      if (object == null) {
        errorConsumer.accept(error(document, uri, "Could not locate " + uri));
        return;
      }
    } else {
      // Query part can carry a string for validation while preserving the rest of the URI for error
      // messages. This is used for propertyName validation where it's not possible to link to the
      // name with a standard JSON Pointer.
      object = query;
    }

    object = rewriteObject(object);

    if (object instanceof Number) {
      Number number = (Number) object;
      if (multipleOf != null && number.doubleValue() / multipleOf.doubleValue() % 1 != 0) {
        errorConsumer.accept(error(document, uri, "Not a multiple"));
      }
      if (maximum != null
          && (exclusiveMaximum instanceof Boolean && (Boolean) exclusiveMaximum
                  ? number.doubleValue() >= maximum.doubleValue()
                  : number.doubleValue() > maximum.doubleValue())) {
        errorConsumer.accept(error(document, uri, "Greater than maximum"));
      }

      if (exclusiveMaximum instanceof Number
          && number.doubleValue() >= ((Number) exclusiveMaximum).doubleValue()) {
        errorConsumer.accept(error(document, uri, "Greater than or equal to exclusive maximum"));
      }
      if (minimum != null
          && (exclusiveMinimum instanceof Boolean && (Boolean) exclusiveMinimum
                  ? number.doubleValue() <= minimum.doubleValue()
                  : number.doubleValue() < minimum.doubleValue())) {
        errorConsumer.accept(error(document, uri, "Less than minimum"));
      }
      if (exclusiveMinimum instanceof Number
          && number.doubleValue() <= ((Number) exclusiveMinimum).doubleValue()) {
        errorConsumer.accept(error(document, uri, "Less than or equal to exclusive minimum"));
      }
      Set<String> okTypes = new HashSet<>();
      okTypes.add("number");
      if (new BigDecimal(number.toString()).remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO)
          == 0) {
        okTypes.add("integer");
      }

      typeCheck(document, uri, okTypes, disallow, errorConsumer);

      if (divisibleBy != null && number.doubleValue() / divisibleBy.doubleValue() % 1 != 0) {
        errorConsumer.accept(error(document, uri, "divisibleBy failed"));
      }

    } else if (object instanceof String) {
      String string = (String) object;
      int unicodeCompliantLength = string.codePointCount(0, string.length());
      if (maxLength != null && unicodeCompliantLength > maxLength.intValue()) {
        errorConsumer.accept(error(document, uri, "Longer than maxLength"));
      }
      if (minLength != null && unicodeCompliantLength < minLength.intValue()) {
        errorConsumer.accept(error(document, uri, "Shorter than minLength"));
      }
      if (pattern != null && !pattern.matches(string)) {
        errorConsumer.accept(error(document, uri, "Pattern did not match"));
      }
      typeCheck(document, uri, setOf("string"), disallow, errorConsumer);
    } else if (object instanceof Boolean) {
      typeCheck(document, uri, setOf("boolean"), disallow, errorConsumer);
    } else if (object instanceof JSONArray) {
      typeCheck(document, uri, setOf("array"), disallow, errorConsumer);
      JSONArray jsonArray = (JSONArray) object;
      if (itemsArray != null) {
        if (jsonArray.length() > itemsArray.size() && additionalItems != null) {
          for (int idx = itemsArray.size(); idx != jsonArray.length(); idx++) {
            additionalItems.validate(
                document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer);
          }
        }

        for (int idx = 0; idx != Math.min(itemsArray.size(), jsonArray.length()); idx++) {
          itemsArray.get(idx).validate(
              document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer);
        }
      }

      if (_items != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          _items.validate(document, PathUtils.append(uri, String.valueOf(idx)), errorConsumer);
        }
      }

      if (maxItems != null && jsonArray.length() > maxItems.intValue()) {
        errorConsumer.accept(error(document, uri, "Above max length"));
      }

      if (minItems != null && jsonArray.length() < minItems.intValue()) {
        errorConsumer.accept(error(document, uri, "Below min items"));
      }

      if (uniqueItems) {
        Collection<Object> items = new HashSet<>();
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          if (!items.add(makeComparable(jsonArray.get(idx)))) {
            errorConsumer.accept(error(document, uri, "Non-unique item found"));
          }
        }
      }

      if (contains != null) {
        boolean onePassed = false;
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          List<ValidationError> errors = new ArrayList<>();
          contains.validate(document, PathUtils.append(uri, String.valueOf(idx)), errors::add);
          if (errors.isEmpty()) {
            onePassed = true;
            break;
          }
        }
        if (!onePassed) {
          errorConsumer.accept(error(document, uri, "No element in the array matched contains"));
        }
      }
    } else if (object instanceof JSONObject) {
      typeCheck(document, uri, setOf("object"), disallow, errorConsumer);
      JSONObject jsonObject = (JSONObject) object;
      if (maxProperties != null && jsonObject.length() > maxProperties.intValue()) {
        errorConsumer.accept(error(document, uri, "Too mamy properties"));
      }
      if (minProperties != null && jsonObject.length() < minProperties.intValue()) {
        errorConsumer.accept(error(document, uri, "Too few properties"));
      }

      for (String property : requiredProperties) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(error(document, uri, "Missing required property " + property));
        }
      }

      Collection<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          Schema schema = _properties.get(property);
          schema.validate(document, PathUtils.append(uri, property), errorConsumer);
          remainingProperties.remove(property);
        }

        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchemas.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern = it0.next();
          Schema schema = it1.next();
          if (pattern.matches(property)) {
            schema.validate(document, PathUtils.append(uri, property), errorConsumer);
            remainingProperties.remove(property);
          }
        }
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
            propertyNames.validate(document, propertyPath, errorConsumer);
          } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
          }
        }
      }
      for (Map.Entry<String, Schema> entry : schemaDependencies.entrySet()) {
        String property = entry.getKey();
        if (!jsonObject.has(property)) {
          continue;
        }

        Schema schema = entry.getValue();
        schema.validate(document, uri, errorConsumer);
      }

      if (additionalProperties != null) {
        for (String property : remainingProperties) {
          additionalProperties.validate(document, PathUtils.append(uri, property), errorConsumer);
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
              error(document, uri, "Missing dependency " + property + " -> " + dependency));
        }
      }

    } else if (object == JSONObject.NULL) {
      typeCheck(document, uri, setOf("null"), disallow, errorConsumer);
    } else {
      errorConsumer.accept(
          error(document, uri, "Cannot validate type " + object.getClass().getSimpleName()));
    }

    if (_const != null) {
      if (!makeComparable(_const).equals(makeComparable(object))) {
        errorConsumer.accept(error(document, uri, "Const mismatch"));
      }
    }

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
        errorConsumer.accept(error(document, uri, "Object not in enum"));
      }
    }

    if (_if != null) {
      List<ValidationError> errors = new ArrayList<>();
      _if.validate(document, uri, errors::add);
      Schema useSchema;
      if (errors.isEmpty()) {
        useSchema = _then;
      } else {
        useSchema = _else;
      }
      if (useSchema != null) {
        useSchema.validate(document, uri, errorConsumer);
      }
    }

    for (Schema schema : allOf) {
      schema.validate(document, uri, errorConsumer);
    }

    if (anyOf != null) {
      boolean onePassed = false;
      for (Schema schema : anyOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(document, uri, errors::add);
        if (errors.isEmpty()) {
          onePassed = true;
          break;
        }
      }
      if (!onePassed) {
        errorConsumer.accept(error(document, uri, "All anyOf failed"));
      }
    }

    if (oneOf != null) {
      int numberPassed = 0;
      for (Schema schema : oneOf) {
        List<ValidationError> errors = new ArrayList<>();
        schema.validate(document, uri, errors::add);
        if (errors.isEmpty()) {
          numberPassed++;
        }
      }
      if (numberPassed != 1) {
        errorConsumer.accept(error(document, uri, numberPassed + " passed oneOf"));
      }
    }

    if (not != null) {
      List<ValidationError> errors = new ArrayList<>();
      not.validate(document, uri, errors::add);
      if (errors.isEmpty()) {
        errorConsumer.accept(error(document, uri, "not condition passed"));
      }
    }

    for (Schema disallowSchema : disallowSchemas) {
      List<ValidationError> errors = new ArrayList<>();
      disallowSchema.validate(document, uri, errors::add);
      if (errors.isEmpty()) {
        errorConsumer.accept(error(document, uri, "disallow condition passed"));
      }
    }
  }

  @Override
  public boolean isObjectSchema() {
    return true;
  }

  @Override
  public ObjectSchema asObjectSchema() {
    return this;
  }

  public Map<String, Schema> getProperties() {
    return Collections.unmodifiableMap(_properties);
  }

  private void typeCheck(Object document, URI path, Set<String> types, Collection<String> disallow,
      Consumer<ValidationError> errorConsumer) {
    Collection<String> typesIn0 = new HashSet<>(types);
    typesIn0.retainAll(disallow);
    if (!typesIn0.isEmpty()) {
      errorConsumer.accept(error(document, path, "Type disallowed"));
    }

    if (explicitTypes == null) {
      return;
    }

    for (Schema schema : typesSchema) {
      List<ValidationError> errors = new ArrayList<>();
      schema.validate(document, path, errors::add);
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

    errorConsumer.accept(error(document, path,
        "Expected: [" + String.join(", ", explicitTypes) + "] "
            + "Found: [" + String.join(", ", types) + "]"));
  }

  public JSONObject getSchemaJson() {
    return schemaJson;
  }

  public Set<String> getExplicitTypes() {
    if (explicitTypes == null) {
      return null;
    }
    return Collections.unmodifiableSet(explicitTypes);
  }

  public Set<String> getInferredTypes() {
    return Collections.unmodifiableSet(inferredTypes);
  }

  public Set<String> getTypes() {
    if (explicitTypes != null) {
      return Collections.unmodifiableSet(explicitTypes);
    }
    if (!allOf.isEmpty()) {
      // For the allOf operator we return the union of the types (implied or explicit) of each
      // subschema.
      Set<String> union = null;
      for (Schema subSchema : allOf) {
        if (!subSchema.isObjectSchema()) {
          continue;
        }
        ObjectSchema objectSubSchema = (ObjectSchema) subSchema;
        Set<String> types = objectSubSchema.getTypes();
        if (union == null) {
          union = new HashSet<>(types);
        } else {
          union.retainAll(types);
        }
      }
      return union;
    }
    return Collections.unmodifiableSet(inferredTypes);
  }

  public Collection<Schema> getItems() {
    Collection<Schema> allItems = new ArrayList<>();
    if (itemsArray != null) {
      allItems.addAll(itemsArray);
    }
    if (_items != null) {
      allItems.add(_items);
    }
    return allItems;
  }

  public Collection<String> getRequiredProperties() {
    return requiredProperties;
  }

  public Object getDefault() {
    return defaultValue;
  }

  public List<Object> getEnums() {
    return enums;
  }

  public Number getMinLength() {
    return minLength;
  }

  public Number getMaxLength() {
    return maxLength;
  }

  public Number getMinItems() {
    return minItems;
  }

  public Number getMaxItems() {
    return maxItems;
  }

  public boolean isFullyBuilt() {
    return fullyBuilt;
  }
}
