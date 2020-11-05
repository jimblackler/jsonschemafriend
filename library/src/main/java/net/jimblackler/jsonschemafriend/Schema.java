package net.jimblackler.jsonschemafriend;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static net.jimblackler.jsonschemafriend.MetaSchemaDetector.detectMetaSchema;
import static net.jimblackler.jsonschemafriend.PathUtils.append;
import static net.jimblackler.jsonschemafriend.PathUtils.fixUnescaped;
import static net.jimblackler.jsonschemafriend.PathUtils.resolve;
import static net.jimblackler.jsonschemafriend.Utils.setOf;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A schema defined by an object. "Object" refers to the type in the definition, not the type of
 * data it validates.
 */
public class Schema {
  private static final Logger LOG = Logger.getLogger(Schema.class.getName());

  private final Object schemaObject; // Kept for debugging only.

  private final SchemaStore schemaStore;
  private final URI uri;
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
  private final String pattern;
  private final String format;
  private final String contentEncoding;
  private final String contentMediaType;
  // array checks
  private final Schema additionalItems;
  private final Schema unevaluatedItems;
  private final Schema _items;
  private final List<Schema> itemsTuple;
  private final Number maxItems;
  private final Number minItems;
  private final boolean uniqueItems;
  private final Schema contains;
  private final Number minContains;
  private final Number maxContains;
  // object checks
  private final Number maxProperties;
  private final Number minProperties;
  private final Collection<String> requiredProperties = new HashSet<>();
  private final boolean required;
  private final Schema additionalProperties;
  private final Schema unevaluatedProperties;
  private final Map<String, Schema> _properties = new LinkedHashMap<>();
  private final Collection<String> patternPropertiesPatterns = new ArrayList<>();
  private final Collection<Schema> patternPropertiesSchemas = new ArrayList<>();
  private final Map<String, Collection<String>> dependentRequired = new HashMap<>();
  private final Map<String, Schema> dependentSchemas = new HashMap<>();
  private final Schema propertyNames;
  // all types checks
  private final Object _const;
  private final List<Object> enums;
  private final Set<String> explicitTypes;
  private final Collection<Schema> typesSchema = new HashSet<>();
  private final Collection<String> disallow = new HashSet<>();
  private final Collection<Schema> disallowSchemas = new HashSet<>();
  private final Object defaultValue;
  // in-place applicators
  private final Schema _if;
  private final Schema _then;
  private final Schema _else;
  private final Collection<Schema> allOf = new ArrayList<>();
  private final Collection<Schema> anyOf;
  private final Collection<Schema> oneOf;
  private final Schema not;
  private final Schema ref;
  private final Schema recursiveRef;
  private final boolean recursiveAnchor;

  private final JSONArray examples;
  private final String title;
  private final String description;

  private URI metaSchema;

  // Own
  private Schema parent;

  Schema(SchemaStore schemaStore, URI uri) throws GenerationException {
    this.schemaStore = schemaStore;
    this.uri = uri;

    schemaStore.register(uri, this);

    Object _schemaObject = schemaStore.getObject(uri);
    if (_schemaObject == null) {
      LOG.warning("No match for " + uri);
      // By design, if we can't find a schema definition, we log a warning but generate a default
      // schema that permits everything.
      _schemaObject = true;
    }

    schemaObject = _schemaObject;
    JSONObject jsonObject;
    if (schemaObject instanceof JSONObject) {
      jsonObject = (JSONObject) schemaObject;
    } else {
      jsonObject = new JSONObject();
    }

    // number checks
    multipleOf = (Number) jsonObject.opt("multipleOf");
    maximum = (Number) jsonObject.opt("maximum");
    exclusiveMaximum = jsonObject.opt("exclusiveMaximum");
    minimum = (Number) jsonObject.opt("minimum");
    exclusiveMinimum = jsonObject.opt("exclusiveMinimum");
    divisibleBy = (Number) jsonObject.opt("divisibleBy");

    // string checks
    maxLength = (Number) jsonObject.opt("maxLength");
    minLength = (Number) jsonObject.opt("minLength");
    Object patternObject = jsonObject.opt("pattern");

    String _pattern = null;
    if (patternObject != null) {
      _pattern = (String) patternObject;
    }
    pattern = _pattern;

    Object formatObject = jsonObject.opt("format");
    format = formatObject instanceof String ? (String) formatObject : null;

    Object contentEncodingObject = jsonObject.opt("contentEncoding");
    contentEncoding =
        contentEncodingObject instanceof String ? (String) contentEncodingObject : null;

    Object contentMediaTypeObject = jsonObject.opt("contentMediaType");
    contentMediaType =
        contentMediaTypeObject instanceof String ? (String) contentMediaTypeObject : null;

    // array checks
    additionalItems = getSubSchema(jsonObject, "additionalItems", uri);
    unevaluatedItems = getSubSchema(jsonObject, "unevaluatedItems", uri);

    Object itemsObject = jsonObject.opt("items");
    URI itemsPath = append(uri, "items");
    if (itemsObject instanceof JSONArray) {
      itemsTuple = new ArrayList<>();
      JSONArray jsonArray = (JSONArray) itemsObject;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        itemsTuple.add(getSubSchema(append(itemsPath, String.valueOf(idx))));
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
    minContains = (Number) jsonObject.opt("minContains");
    maxContains = (Number) jsonObject.opt("maxContains");

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
    required = requiredObject instanceof Boolean && (Boolean) requiredObject;

    additionalProperties = getSubSchema(jsonObject, "additionalProperties", uri);
    unevaluatedProperties = getSubSchema(jsonObject, "unevaluatedProperties", uri);

    Object propertiesObject = jsonObject.opt("properties");
    if (propertiesObject instanceof JSONObject) {
      JSONObject properties = (JSONObject) propertiesObject;
      URI propertiesPointer = append(uri, "properties");
      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        URI propertyUri = append(propertiesPointer, propertyName);
        _properties.put(propertyName, getSubSchema(propertyUri));
      }
    }

    Object patternPropertiesObject = jsonObject.opt("patternProperties");
    if (patternPropertiesObject instanceof JSONObject) {
      JSONObject patternProperties = (JSONObject) patternPropertiesObject;
      URI propertiesPointer = append(uri, "patternProperties");
      Iterator<String> it = patternProperties.keys();
      while (it.hasNext()) {
        String propertyPattern = it.next();
        patternPropertiesPatterns.add(propertyPattern);
        URI patternPointer = append(propertiesPointer, propertyPattern);
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
          dependentRequired.put(dependency, spec);
        } else if (dependencyObject instanceof JSONObject || dependencyObject instanceof Boolean) {
          URI dependenciesPointer = append(uri, "dependencies");
          dependentSchemas.put(dependency, getSubSchema(append(dependenciesPointer, dependency)));
        } else {
          Collection<String> objects = new ArrayList<>();
          objects.add((String) dependencyObject);
          dependentRequired.put(dependency, objects);
        }
      }
    }

    JSONObject dependentRequiredJsonObject = jsonObject.optJSONObject("dependentRequired");
    if (dependentRequiredJsonObject != null) {
      for (String dependency : dependentRequiredJsonObject.keySet()) {
        Collection<String> spec = new ArrayList<>();
        JSONArray array = (JSONArray) dependentRequiredJsonObject.get(dependency);
        for (int idx = 0; idx != array.length(); idx++) {
          spec.add(array.getString(idx));
        }
        dependentRequired.put(dependency, spec);
      }
    }

    JSONObject dependentSchemasJsonObject = jsonObject.optJSONObject("dependentSchemas");
    if (dependentSchemasJsonObject != null) {
      for (String dependency : dependentSchemasJsonObject.keySet()) {
        URI dependenciesPointer = append(uri, "dependentSchemas");
        dependentSchemas.put(dependency, getSubSchema(append(dependenciesPointer, dependency)));
      }
    }

    propertyNames = getSubSchema(jsonObject, "propertyNames", uri);

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
      }
    }

    Object typeObject = jsonObject.opt("type");
    if (typeObject instanceof JSONArray) {
      URI typePointer = append(uri, "type");
      explicitTypes = new HashSet<>();
      JSONArray array = (JSONArray) typeObject;
      for (int idx = 0; idx != array.length(); idx++) {
        Object arrayEntryObject = array.get(idx);
        if (arrayEntryObject instanceof Boolean || arrayEntryObject instanceof JSONObject) {
          typesSchema.add(getSubSchema(append(typePointer, String.valueOf(idx))));
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
      URI arrayPath = append(uri, "allOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(getSubSchema(indexPointer));
      }
    }

    Object extendsObject = jsonObject.opt("extends");
    if (extendsObject instanceof JSONArray) {
      URI arrayPath = append(uri, "extends");
      JSONArray array = (JSONArray) extendsObject;
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(getSubSchema(indexPointer));
      }
    } else if (extendsObject instanceof JSONObject || extendsObject instanceof Boolean) {
      URI arrayPath = append(uri, "extends");
      allOf.add(getSubSchema(arrayPath));
    }

    Object refObject = jsonObject.opt("$ref");
    if (refObject instanceof String) {
      // Refs should be URL Escaped already; but in practice they are sometimes not.
      URI resolved = resolve(uri, URI.create(fixUnescaped((String) refObject)));
      ref = schemaStore.loadSchema(resolved, false);
    } else {
      ref = null;
    }

    Object recursiveRefObject = jsonObject.opt("$recursiveRef");
    if (recursiveRefObject instanceof String) {
      URI resolved = resolve(uri, URI.create((String) recursiveRefObject));
      recursiveRef = schemaStore.loadSchema(resolved, false);
    } else {
      recursiveRef = null;
    }

    recursiveAnchor = jsonObject.optBoolean("$recursiveAnchor");

    Object anyOfObject = jsonObject.opt("anyOf");
    if (anyOfObject instanceof JSONArray) {
      anyOf = new ArrayList<>();
      JSONArray array = (JSONArray) anyOfObject;
      URI arrayPath = append(uri, "anyOf");
      for (int idx = 0; idx != array.length(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        anyOf.add(getSubSchema(indexPointer));
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
      URI disallowPointer = append(uri, "disallow");
      for (int idx = 0; idx != array.length(); idx++) {
        Object disallowEntryObject = array.get(idx);
        if (disallowEntryObject instanceof String) {
          disallow.add(array.getString(idx));
        } else {
          disallowSchemas.add(getSubSchema(append(disallowPointer, String.valueOf(idx))));
        }
      }
    }

    defaultValue = jsonObject.opt("default");
    title = jsonObject.optString("title");
    description = jsonObject.optString("description");
    examples = jsonObject.optJSONArray("examples");

    if (examples != null) {
      Validator validator = new Validator();
      for (int idx = 0; idx != examples.length(); idx++) {
        Object example = examples.get(idx);
        try {
          validator.validate(this, example);
        } catch (ValidationException e) {
          throw new GenerationException(e);
        }
      }
    }
  }

  private Schema getSubSchema(JSONObject jsonObject, String name, URI uri)
      throws GenerationException {
    Object childObject = jsonObject.opt(name);
    if (childObject instanceof JSONObject || childObject instanceof Boolean) {
      return getSubSchema(append(uri, name));
    }
    return null;
  }

  private Schema getSubSchema(URI uri) throws GenerationException {
    Schema subSchema = schemaStore.loadSchema(uri, false);
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

  @Override
  public String toString() {
    return uri + " / " + schemaObject;
  }

  public boolean isExclusiveMinimumBoolean() {
    if (exclusiveMinimum instanceof Boolean) {
      return (Boolean) exclusiveMinimum;
    }
    return false;
  }

  public boolean isExclusiveMaximumBoolean() {
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

  public String getPattern() {
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

  public Schema getUnevaluatedItems() {
    return unevaluatedItems;
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

  public Schema getContains() {
    return contains;
  }

  public boolean isUniqueItems() {
    return uniqueItems;
  }

  public Number getMinContains() {
    return minContains;
  }

  public Number getMaxContains() {
    return maxContains;
  }

  public Number getMaxProperties() {
    return maxProperties;
  }

  public Number getMinProperties() {
    return minProperties;
  }

  public Collection<String> getRequiredProperties() {
    return unmodifiableCollection(requiredProperties);
  }

  public boolean isRequired() {
    return required;
  }

  public Schema getAdditionalProperties() {
    return additionalProperties;
  }

  public Schema getUnevaluatedProperties() {
    return unevaluatedProperties;
  }

  public Map<String, Schema> getProperties() {
    return unmodifiableMap(_properties);
  }

  public Collection<String> getPatternPropertiesPatterns() {
    return unmodifiableCollection(patternPropertiesPatterns);
  }

  public Collection<Schema> getPatternPropertiesSchema() {
    return unmodifiableCollection(patternPropertiesSchemas);
  }

  public Map<String, Collection<String>> getDependentRequired() {
    return unmodifiableMap(dependentRequired);
  }

  public Map<String, Schema> getDependentSchemas() {
    return unmodifiableMap(dependentSchemas);
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

  public Collection<Schema> getTypesSchema() {
    return unmodifiableCollection(typesSchema);
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
    return unmodifiableCollection(allOf);
  }

  public Collection<Schema> getAnyOf() {
    return anyOf == null ? null : unmodifiableCollection(anyOf);
  }

  public Collection<Schema> getOneOf() {
    return oneOf == null ? null : unmodifiableCollection(oneOf);
  }

  public Schema getNot() {
    return not;
  }

  public Schema getRef() {
    return ref;
  }

  public boolean isRecursiveAnchor() {
    return recursiveAnchor;
  }

  public Schema getRecursiveRef() {
    return recursiveRef;
  }

  public Collection<String> getDisallow() {
    return unmodifiableCollection(disallow);
  }

  public Collection<Schema> getDisallowSchemas() {
    return unmodifiableCollection(disallowSchemas);
  }

  public Object getDefault() {
    return defaultValue;
  }

  public JSONArray getExamples() {
    return examples;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
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

  public URI getMetaSchema() {
    if (metaSchema == null) {
      metaSchema = detectMetaSchema(schemaStore.getBaseObject(uri));
    }
    return metaSchema;
  }

  @Override
  public int hashCode() {
    return uri.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Schema)) {
      return false;
    }
    return uri.equals(((Schema) obj).getUri());
  }
}
