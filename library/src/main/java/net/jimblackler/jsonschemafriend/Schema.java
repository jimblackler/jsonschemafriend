package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.Utils.setOf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;

/**
 * A schema defined by an object. "Object" refers to the type in the definition, not the type of
 * data it validates.
 */
public class Schema {
  private static final Logger LOG = Logger.getLogger(Schema.class.getName());

  private final Object schemaObject; // Kept for debugging only.

  private final SchemaStore schemaStore;
  private final URI uri;
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
  private final String format;
  private final String contentEncoding;
  private final String contentMediaType;
  // array checks
  private final Schema additionalItems;
  private final Schema _items;
  private final List<Schema> itemsTuple;
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
  private final Collection<String> disallow = new HashSet<>();
  private final Collection<Schema> disallowSchemas = new HashSet<>();
  private final Object defaultValue;
  private final boolean fullyBuilt;
  private Schema parent;

  public Schema(SchemaStore schemaStore, URI uri, URI defaultMetaSchema)
      throws GenerationException {
    this.uri = uri;
    this.schemaStore = schemaStore;
    schemaStore.register(uri, this);

    URI baseDocumentUri = PathUtils.baseDocumentFromUri(uri);
    Object base;

    try {
      base = schemaStore.getDocumentSource().fetchDocument(baseDocumentUri);
    } catch (MissingPathException e) {
      // By design, if we can't find a schema definition, we log a warning but generate a default
      // schema that permits everything.
      LOG.warning("No document found at " + baseDocumentUri);
      base = true;
    }
    if (base instanceof JSONObject) {
      JSONObject baseDocument = (JSONObject) base;
      Object _schemaObject = null;
      try {
        _schemaObject = PathUtils.fetchFromPath(baseDocument, uri.getRawFragment());
      } catch (MissingPathException e) {
        LOG.warning(
            "No match for path " + uri.getRawFragment() + " in document " + baseDocumentUri);
      }

      if (_schemaObject == null) {
        // By design, if we can't find a schema definition, we log a warning but generate a default
        // schema that permits everything.
        metaSchemaUri = defaultMetaSchema;
        schemaObject = true;
      } else {
        schemaObject = _schemaObject;
        Object _metaSchema = baseDocument.opt("$schema");
        if (_metaSchema instanceof String) {
          metaSchemaUri = URI.create((String) _metaSchema);
        } else {
          metaSchemaUri = defaultMetaSchema;
        }
      }
    } else {
      metaSchemaUri = defaultMetaSchema;
      schemaObject = base;
    }
    // It would be more convenient to work with a fully-built Schema from the meta-schema, not just
    // its JSON representation. However that isn't possible when building a self-referencing schema
    // (all JSON schema meta-schemas as self-referencing).
    JSONObject metaSchemaDocument;

    try {
      metaSchemaDocument =
          (JSONObject) schemaStore.getDocumentSource().fetchDocument(metaSchemaUri);
    } catch (MissingPathException e) {
      LOG.warning("Could not load metaschema " + metaSchemaUri);
      metaSchemaDocument = null;
    }

    // If possible, create a new version of the object with only the properties that are explicitly
    // in the metaschema. This means that features from other version of the metaschema from working
    // when they shouldn't.
    JSONObject jsonObject = new JSONObject();

    if (schemaObject instanceof JSONObject) {
      JSONObject jsonObjectOriginal = (JSONObject) schemaObject;
      if (metaSchemaDocument == null) {
        for (String property : jsonObjectOriginal.keySet()) {
          jsonObject.put(property, jsonObjectOriginal.get(property));
        }
      } else {
        JSONObject properties = metaSchemaDocument.optJSONObject("properties");
        if (properties != null) {
          for (String property : properties.keySet()) {
            if (jsonObjectOriginal.has(property)) {
              jsonObject.put(property, jsonObjectOriginal.get(property));
            }
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
    Object formatObject = jsonObject.opt("format");
    format = formatObject instanceof String ? (String) formatObject : null;

    Object contentEncodingObject = jsonObject.opt("contentEncoding");
    contentEncoding =
        contentEncodingObject instanceof String ? (String) contentEncodingObject : null;

    Object contentMediaTypeObject = jsonObject.opt("contentMediaType");
    contentMediaType =
        contentMediaTypeObject instanceof String ? (String) contentMediaTypeObject : null;

    if (jsonObject.has("maxLength") || jsonObject.has("minLength") || jsonObject.has("pattern")
        || jsonObject.has("format") || jsonObject.has("contentEncoding")
        || jsonObject.has("contentMediaType")) {
      inferredTypes.add("string");
    }
    // array checks
    additionalItems = getSubSchema(jsonObject, "additionalItems", uri);

    Object itemsObject = jsonObject.opt("items");
    URI itemsPath = PathUtils.append(uri, "items");
    if (itemsObject instanceof JSONArray) {
      itemsTuple = new ArrayList<>();
      JSONArray jsonArray = (JSONArray) itemsObject;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        itemsTuple.add(getSubSchema(PathUtils.append(itemsPath, String.valueOf(idx))));
      }
      _items = null;
    } else {
      itemsTuple = null;
      _items = getSubSchema(jsonObject, "items", uri);
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
        Object propertyObject = new JSONPointer("#" + propertyUri.getRawFragment()).queryFrom(base);
        if (propertyObject instanceof JSONObject) {
          JSONObject propertyJsonObject = (JSONObject) propertyObject;
          Object required = propertyJsonObject.opt("required");
          if (required instanceof Boolean && (Boolean) required) {
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

  private static Collection<String> allTypes() {
    Collection<String> types = new HashSet<>();
    types.add("array");
    types.add("boolean");
    types.add("integer");
    types.add("null");
    types.add("number");
    types.add("object");
    types.add("string");
    return types;
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

  public Boolean isFalse() {
    if (schemaObject instanceof Boolean) {
      return !(Boolean) schemaObject;
    }
    return false;
  }

  public Collection<String> getNonProhibitedTypes() {
    if (explicitTypes == null) {
      return allTypes();
    }
    return Collections.unmodifiableSet(explicitTypes);
  }

  @Override
  public String toString() {
    return uri + " / " + schemaObject;
  }

  public boolean isUniqueItems() {
    return uniqueItems;
  }

  public Boolean getExclusiveMinimumBoolean() {
    if (exclusiveMinimum instanceof Boolean) {
      return (Boolean) exclusiveMinimum;
    }
    return false;
  }

  public Boolean getExclusiveMaximumBoolean() {
    if (exclusiveMaximum instanceof Boolean) {
      return (Boolean) exclusiveMaximum;
    }
    return false;
  }

  public Object getSchemaObject() {
    return schemaObject;
  }

  public URI getUri() {
    return uri;
  }

  public Number getMultipleOf() {
    return multipleOf;
  }

  public Number getMaximum() {
    return maximum;
  }

  public Number getExclusiveMaximum() {
    if (exclusiveMaximum instanceof Number) {
      return (Number) exclusiveMaximum;
    }
    return null;
  }

  public Number getMinimum() {
    return minimum;
  }

  public Number getExclusiveMinimum() {
    if (exclusiveMinimum instanceof Number) {
      return (Number) exclusiveMinimum;
    }
    return null;
  }

  public Number getDivisibleBy() {
    return divisibleBy;
  }

  public Number getMaxLength() {
    return maxLength;
  }

  public Number getMinLength() {
    return minLength;
  }

  public Ecma262Pattern getPattern() {
    return pattern;
  }

  public String getFormat() {
    return format;
  }

  public String getContentEncoding() {
    return contentEncoding;
  }

  public String getContentMediaType() {
    return contentMediaType;
  }

  public Schema getAdditionalItems() {
    return additionalItems;
  }

  public Schema getItems() {
    return _items;
  }

  public List<Schema> getItemsTuple() {
    return itemsTuple == null ? null : Collections.unmodifiableList(itemsTuple);
  }

  public Number getMaxItems() {
    return maxItems;
  }

  public Number getMinItems() {
    return minItems;
  }

  public boolean getUniqueItems() {
    return uniqueItems;
  }

  public Schema getContains() {
    return contains;
  }

  public Number getMaxProperties() {
    return maxProperties;
  }

  public Number getMinProperties() {
    return minProperties;
  }

  public Collection<String> getRequiredProperties() {
    return Collections.unmodifiableCollection(requiredProperties);
  }

  public Schema getAdditionalProperties() {
    return additionalProperties;
  }

  public Map<String, Schema> getProperties() {
    return Collections.unmodifiableMap(_properties);
  }

  public Collection<Ecma262Pattern> getPatternPropertiesPatterns() {
    return Collections.unmodifiableCollection(patternPropertiesPatterns);
  }

  public Collection<Schema> getPatternPropertiesSchema() {
    return Collections.unmodifiableCollection(patternPropertiesSchemas);
  }

  public Map<String, Collection<String>> getDependencies() {
    return dependencies;
  }

  public Map<String, Schema> getSchemaDependencies() {
    return schemaDependencies;
  }

  public Schema getPropertyNames() {
    return propertyNames;
  }

  public Object getConst() {
    return _const;
  }

  public List<Object> getEnums() {
    return enums == null ? null : Collections.unmodifiableList(enums);
  }

  public Collection<String> getExplicitTypes() {
    if (explicitTypes == null) {
      return null;
    }
    return Collections.unmodifiableSet(explicitTypes);
  }

  public Collection<String> getInferredTypes() {
    // TODO: this could be made an external static method.
    if (explicitTypes != null) {
      return Collections.unmodifiableSet(explicitTypes);
    }
    if (anyOf != null && !anyOf.isEmpty()) {
      // For the anyOf operator we return the intersection of the types (implied or explicit) of
      // each subschema.
      Collection<String> intersection = new HashSet<>();
      for (Schema subSchema : anyOf) {
        intersection.addAll(subSchema.getInferredTypes());
      }
      return intersection;
    }
    if (!allOf.isEmpty()) {
      // For the allOf operator we return the union of the types (implied or explicit) of each
      // subschema.
      Set<String> union = null;
      for (Schema subSchema : allOf) {
        Collection<String> types = subSchema.getInferredTypes();
        if (union == null) {
          union = new HashSet<>(types);
        } else {
          union.retainAll(types);
        }
      }
      if (union.isEmpty()) {
        return null;
      }
      return union;
    }

    if (inferredTypes.isEmpty()) {
      // If type inference found nothing, we don't want to imply no types are allowed.
      return allTypes();
    }
    return Collections.unmodifiableSet(inferredTypes);
  }

  public Collection<Schema> getTypesSchema() {
    return typesSchema;
  }

  public Schema getIf() {
    return _if;
  }

  public Schema getThen() {
    return _then;
  }

  public Schema getElse() {
    return _else;
  }

  public Collection<Schema> getAllOf() {
    return Collections.unmodifiableCollection(allOf);
  }

  public Collection<Schema> getAnyOf() {
    return anyOf == null ? null : Collections.unmodifiableCollection(anyOf);
  }

  public Collection<Schema> getOneOf() {
    return oneOf == null ? null : Collections.unmodifiableCollection(oneOf);
  }

  public Schema getNot() {
    return not;
  }

  public Collection<String> getDisallow() {
    return disallow;
  }

  public Collection<Schema> getDisallowSchemas() {
    return disallowSchemas;
  }

  public Object getDefault() {
    return defaultValue;
  }

  public boolean isFullyBuilt() {
    return fullyBuilt;
  }

  public Schema getParent() {
    return parent;
  }

  protected void setParent(Schema parent) {
    if (this.parent != null) {
      throw new IllegalStateException("Schemas may only have one parent");
    }
    this.parent = parent;
  }
}
