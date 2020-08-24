package net.jimblackler.jsonschematypes;

import static net.jimblackler.jsonschematypes.ComparableMutable.makeComparable;
import static net.jimblackler.jsonschematypes.PathUtils.append;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONPointer;

public class ObjectSchema extends Schema {
  private final JSONObject schemaJson; // Kept for debugging only.

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
  private final Collection<String> required = new HashSet<>();
  private final Schema additionalProperties;
  private final Map<String, Schema> _properties = new HashMap<>();
  private final Collection<Ecma262Pattern> patternPropertiesPatterns = new ArrayList<>();
  private final Collection<Schema> patternPropertiesSchemas = new ArrayList<>();
  private final Map<String, Collection<String>> dependencies = new HashMap<>();
  private final Map<String, Schema> schemaDependencies = new HashMap<>();
  private final Schema propertyNames;

  // all types checks
  private final Object _const;
  private final Set<Object> _enum;
  private final Set<String> types;
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

  public ObjectSchema(SchemaStore schemaStore, URI uri, URI defaultMetaSchema)
      throws GenerationException {
    super(schemaStore, uri);

    URI baseDocumentUri = PathUtils.baseDocumentFromUri(uri);
    JSONObject baseDocument =
        (JSONObject) schemaStore.getDocumentSource().fetchDocument(baseDocumentUri);
    JSONObject jsonObjectOriginal =
        (JSONObject) PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());

    URI metaSchemaUri;
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
      JSONObject props = metaSchemaDocument.getJSONObject("properties");
      for (String property : props.keySet()) {
        if (jsonObjectOriginal.has(property)) {
          jsonObject.put(property, jsonObjectOriginal.get(property));
        }
      }
    }

    // number checks
    multipleOf = (Number) jsonObject.opt("multipleOf");
    maximum = (Number) jsonObject.opt("maximum");
    exclusiveMaximum = jsonObject.opt("exclusiveMaximum");
    minimum = (Number) jsonObject.opt("minimum");
    exclusiveMinimum = jsonObject.opt("exclusiveMinimum");
    maxLength = (Number) jsonObject.opt("maxLength");
    minLength = (Number) jsonObject.opt("minLength");
    divisibleBy = (Number) jsonObject.opt("divisibleBy");

    // string checks
    Object patternObject = jsonObject.opt("pattern");
    if (patternObject == null) {
      pattern = null;
    } else {
      pattern = new Ecma262Pattern((String) patternObject);
    }

    // array checks
    additionalItems = getSchema(jsonObject, "additionalItems", schemaStore, defaultMetaSchema, uri);
    _items = getSchema(jsonObject, "items", schemaStore, defaultMetaSchema, uri);

    Object itemsObject = jsonObject.opt("items");
    URI itemsPath = append(uri, "items");
    if (itemsObject instanceof JSONArray) {
      itemsArray = new ArrayList<>();
      JSONArray jsonArray = (JSONArray) itemsObject;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        itemsArray.add(
            schemaStore.getSchema(append(itemsPath, String.valueOf(idx)), defaultMetaSchema));
      }
    } else {
      itemsArray = null;
    }

    maxItems = (Number) jsonObject.opt("maxItems");
    minItems = (Number) jsonObject.opt("minItems");
    uniqueItems = jsonObject.optBoolean("uniqueItems", false);
    contains = getSchema(jsonObject, "contains", schemaStore, defaultMetaSchema, uri);

    // object checks
    maxProperties = (Number) jsonObject.opt("maxProperties");
    minProperties = (Number) jsonObject.opt("minProperties");

    Object requiredObject = jsonObject.opt("required");
    if (requiredObject instanceof JSONArray) {
      JSONArray array = (JSONArray) requiredObject;
      for (int idx = 0; idx != array.length(); idx++) {
        required.add(array.getString(idx));
      }
    }

    additionalProperties =
        getSchema(jsonObject, "additionalProperties", schemaStore, defaultMetaSchema, uri);

    Object propertiesObject = jsonObject.opt("properties");
    if (propertiesObject instanceof JSONObject) {
      JSONObject properties = (JSONObject) propertiesObject;
      URI propertiesPointer = append(uri, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        URI propertyUri = append(propertiesPointer, propertyName);

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
            required.add(propertyName);
          }
        }

        _properties.put(propertyName, schemaStore.getSchema(propertyUri, defaultMetaSchema));
      }
    }

    Object patternPropertiesObject = jsonObject.opt("patternProperties");
    if (patternPropertiesObject instanceof JSONObject) {
      JSONObject patternProperties = (JSONObject) patternPropertiesObject;
      URI propertiesPointer = append(uri, "patternProperties");
      Iterator<String> it = patternProperties.keys();
      while (it.hasNext()) {
        String propertyPattern = it.next();
        patternPropertiesPatterns.add(new Ecma262Pattern(propertyPattern));
        patternPropertiesSchemas.add(
            schemaStore.getSchema(append(propertiesPointer, propertyPattern), defaultMetaSchema));
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
          URI dependenciesPpinter = append(uri, "dependencies");
          schemaDependencies.put(dependency,
              schemaStore.getSchema(append(dependenciesPpinter, dependency), defaultMetaSchema));
        } else {
          dependencies.put(dependency, List.of((String) dependencyObject));
        }
      }
    }

    propertyNames = getSchema(jsonObject, "propertyNames", schemaStore, defaultMetaSchema, uri);

    // all types checks
    _const = jsonObject.opt("const");

    JSONArray enumArray = jsonObject.optJSONArray("enum");
    if (enumArray == null) {
      _enum = null;
    } else {
      _enum = new HashSet<>();
      for (int idx = 0; idx != enumArray.length(); idx++) {
        _enum.add(enumArray.get(idx));
      }
    }

    Object typeObject = jsonObject.opt("type");
    if (typeObject instanceof JSONArray) {
      URI typePointer = append(uri, "type");
      types = new HashSet<>();
      JSONArray array = (JSONArray) typeObject;
      for (int idx = 0; idx != array.length(); idx++) {
        Object arrayEntryObject = array.get(idx);
        if (arrayEntryObject instanceof Boolean || arrayEntryObject instanceof JSONObject) {
          typesSchema.add(
              schemaStore.getSchema(append(typePointer, String.valueOf(idx)), defaultMetaSchema));
        } else {
          types.add((String) arrayEntryObject);
        }
      }
    } else if (typeObject instanceof String) {
      types = Set.of(typeObject.toString());
    } else {
      types = null;
    }

    _if = getSchema(jsonObject, "if", schemaStore, defaultMetaSchema, uri);
    _then = getSchema(jsonObject, "then", schemaStore, defaultMetaSchema, uri);
    _else = getSchema(jsonObject, "else", schemaStore, defaultMetaSchema, uri);

    Object allOfObject = jsonObject.opt("allOf");
    if (allOfObject instanceof JSONArray) {
      JSONArray array = (JSONArray) allOfObject;
      URI arrayPath = append(uri, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(schemaStore.getSchema(indexPointer, defaultMetaSchema));
      }
    }

    Object extendsObject = jsonObject.opt("extends");
    if (extendsObject instanceof JSONArray) {
      URI arrayPath = append(uri, "extends");
      JSONArray array = (JSONArray) extendsObject;
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(schemaStore.getSchema(indexPointer, defaultMetaSchema));
      }
    } else if (extendsObject instanceof JSONObject || extendsObject instanceof Boolean) {
      URI arrayPath = append(uri, "extends");
      allOf.add(schemaStore.getSchema(arrayPath, defaultMetaSchema));
    }

    Object anyOfObject = jsonObject.opt("anyOf");
    if (anyOfObject instanceof JSONArray) {
      anyOf = new ArrayList<>();
      JSONArray array = (JSONArray) anyOfObject;
      URI arrayPath = append(uri, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        anyOf.add(schemaStore.getSchema(indexPointer, defaultMetaSchema));
      }
    } else {
      anyOf = null;
    }

    Object oneOfObject = jsonObject.opt("oneOf");
    if (oneOfObject instanceof JSONArray) {
      oneOf = new ArrayList<>();
      JSONArray array = (JSONArray) oneOfObject;
      URI arrayPath = append(uri, "oneOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        oneOf.add(schemaStore.getSchema(indexPointer, defaultMetaSchema));
      }
    } else {
      oneOf = null;
    }

    not = getSchema(jsonObject, "not", schemaStore, defaultMetaSchema, uri);

    Object disallowObject = jsonObject.opt("disallow");
    URI disallowPointer = append(uri, "disallow");
    if (disallowObject instanceof String) {
      disallow.add(disallowObject.toString());
    } else if (disallowObject instanceof JSONArray) {
      JSONArray array = (JSONArray) disallowObject;
      for (int idx = 0; idx != array.length(); idx++) {
        Object disallowEntryObject = array.get(idx);
        if (disallowEntryObject instanceof String) {
          disallow.add(array.getString(idx));
        } else {
          disallowSchemas.add(schemaStore.getSchema(
              append(disallowPointer, String.valueOf(idx)), defaultMetaSchema));
        }
      }
    }
  }

  private static Schema getSchema(JSONObject jsonObject, String name, SchemaStore schemaStore,
      URI defaultMetaSchema, URI uri) throws GenerationException {
    Object object = jsonObject.opt(name);
    if (object instanceof JSONObject || object instanceof Boolean) {
      return schemaStore.getSchema(append(uri, name), defaultMetaSchema);
    }
    return null;
  }

  @Override
  public void validate(Object document, URI uri, Consumer<ValidationError> errorConsumer) {
    Object object;
    String query = uri.getQuery();
    if (query == null || query.isEmpty()) {
      object = PathUtils.fetchFromPath(document, uri.getRawFragment());
      if (object == null) {
        errorConsumer.accept(error(document, uri, "Could not locate " + uri));
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
      typeCheck(document, uri, Set.of("string"), disallow, errorConsumer);
    } else if (object instanceof Boolean) {
      typeCheck(document, uri, Set.of("boolean"), disallow, errorConsumer);
    } else if (object instanceof JSONArray) {
      typeCheck(document, uri, Set.of("array"), disallow, errorConsumer);
      JSONArray jsonArray = (JSONArray) object;
      if (itemsArray != null) {
        if (jsonArray.length() > itemsArray.size() && additionalItems != null) {
          for (int idx = itemsArray.size(); idx != jsonArray.length(); idx++) {
            additionalItems.validate(document, append(uri, String.valueOf(idx)), errorConsumer);
          }
        }

        for (int idx = 0; idx != Math.min(itemsArray.size(), jsonArray.length()); idx++) {
          itemsArray.get(idx).validate(document, append(uri, String.valueOf(idx)), errorConsumer);
        }
      }

      if (_items != null) {
        for (int idx = 0; idx != jsonArray.length(); idx++) {
          _items.validate(document, append(uri, String.valueOf(idx)), errorConsumer);
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
          contains.validate(document, append(uri, String.valueOf(idx)), errors::add);
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
      typeCheck(document, uri, Set.of("object"), disallow, errorConsumer);
      JSONObject jsonObject = (JSONObject) object;
      if (maxProperties != null && jsonObject.length() > maxProperties.intValue()) {
        errorConsumer.accept(error(document, uri, "Too mamy properties"));
      }
      if (minProperties != null && jsonObject.length() < minProperties.intValue()) {
        errorConsumer.accept(error(document, uri, "Too few properties"));
      }

      for (String property : required) {
        if (!jsonObject.has(property)) {
          errorConsumer.accept(error(document, uri, "Missing required property " + property));
        }
      }

      Collection<String> remainingProperties = new HashSet<>(jsonObject.keySet());
      for (String property : jsonObject.keySet()) {
        if (_properties.containsKey(property)) {
          Schema schema = _properties.get(property);
          schema.validate(document, append(uri, property), errorConsumer);
          remainingProperties.remove(property);
        }

        Iterator<Ecma262Pattern> it0 = patternPropertiesPatterns.iterator();
        Iterator<Schema> it1 = patternPropertiesSchemas.iterator();
        while (it0.hasNext()) {
          Ecma262Pattern pattern = it0.next();
          Schema schema = it1.next();
          if (pattern.matches(property)) {
            schema.validate(document, append(uri, property), errorConsumer);
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
                uri.getScheme(), uri.getAuthority(), uri.getPath(), property, uri.getFragment());
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
          additionalProperties.validate(document, append(uri, property), errorConsumer);
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
      typeCheck(document, uri, Set.of("null"), disallow, errorConsumer);
    } else {
      errorConsumer.accept(
          error(document, uri, "Cannot validate type " + object.getClass().getSimpleName()));
    }

    if (_const != null) {
      if (!makeComparable(_const).equals(makeComparable(object))) {
        errorConsumer.accept(error(document, uri, "Const mismatch"));
      }
    }

    if (_enum != null) {
      boolean matchedOne = false;
      Object o = makeComparable(object);
      for (Object value : _enum) {
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

  private Object rewriteObject(Object object) {
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

  private void typeCheck(Object document, URI path, Set<String> types, Set<String> disallow,
      Consumer<ValidationError> errorConsumer) {
    Set<String> typesIn0 = new HashSet<>(types);
    typesIn0.retainAll(disallow);
    if (!typesIn0.isEmpty()) {
      errorConsumer.accept(error(document, path, "Type disallowed"));
    }

    if (this.types == null) {
      return;
    }

    for (Schema schema : typesSchema) {
      List<ValidationError> errors = new ArrayList<>();
      schema.validate(document, path, errors::add);
      if (errors.isEmpty()) {
        return;
      }
    }

    if (this.types.contains("any")) {
      return;
    }

    Set<String> typesIn = new HashSet<>(types);
    typesIn.retainAll(this.types);
    if (!typesIn.isEmpty()) {
      return;
    }

    String part1;
    if (types.size() == 1) {
      part1 = "type " + types.iterator().next() + " ";
    } else {
      part1 = "one of: " + String.join(", ", types) + " ";
    }

    String part2;
    if (this.types.size() == 1) {
      part2 = "not " + this.types.iterator().next();
    } else {
      part2 = "not one of: " + String.join(", ", this.types);
    }

    errorConsumer.accept(error(document, path, "Object is " + part1 + part2));
  }
}
