package net.jimblackler.jsonschemafriend;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static net.jimblackler.jsonschemafriend.MetaSchemaDetector.detectMetaSchema;
import static net.jimblackler.jsonschemafriend.PathUtils.append;
import static net.jimblackler.jsonschemafriend.PathUtils.fixUnescaped;
import static net.jimblackler.jsonschemafriend.PathUtils.resolve;
import static net.jimblackler.jsonschemafriend.Utils.getOrDefault;
import static net.jimblackler.jsonschemafriend.Utils.setOf;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * A schema defined by an object. "Object" refers to the type in the definition, not the type of
 * data it validates.
 */
public class Schema {
  private static final Logger LOG = Logger.getLogger(Schema.class.getName());

  private final Object schemaObject; // Kept for debugging only.
  private final Object baseObject; // Kept to infer the metaschema if required.

  private final URI uri;
  private final URI resourceUri;
  private final boolean isFalse;

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
  private final List<Schema> prefixItems;
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
  private final boolean hasConst;
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
  private final URI dynamicRefURI;
  private final Schema defaultDynamicRef;
  private final String dynamicAnchor;
  private final boolean recursiveAnchor;
  private final Map<String, Schema> dynamicAnchorsInResource = new HashMap<>();
  private final List<Object> examples;
  private final String title;
  private final String description;

  private URI metaSchema;

  // Own
  private Map<URI, Schema> subSchemas = new LinkedHashMap<>();
  private Schema parent;

  Schema(SchemaStore schemaStore, URI uri) throws GenerationException {
    this.uri = uri;
    URI resourceUri = schemaStore.canonicalUriToResourceUri(uri);
    this.resourceUri = resourceUri == null ? uri : resourceUri;

    schemaStore.register(uri, this);

    Object _schemaObject = schemaStore.getObject(uri);
    if (_schemaObject == null) {
      LOG.warning("No match for " + uri);
      // By design, if we can't find a schema definition, we log a warning but generate a default
      // schema that permits everything.
      _schemaObject = true;
    }

    schemaObject = _schemaObject;
    Map<String, Object> jsonObject;
    if (schemaObject instanceof Map) {
      jsonObject = (Map<String, Object>) schemaObject;
    } else {
      jsonObject = new LinkedHashMap<>();
    }

    baseObject = schemaStore.getBaseObject(uri);

    isFalse = Boolean.FALSE.equals(schemaObject);

    // number checks
    multipleOf = (Number) jsonObject.get("multipleOf");
    maximum = (Number) jsonObject.get("maximum");
    exclusiveMaximum = jsonObject.get("exclusiveMaximum");
    minimum = (Number) jsonObject.get("minimum");
    exclusiveMinimum = jsonObject.get("exclusiveMinimum");
    divisibleBy = (Number) jsonObject.get("divisibleBy");

    // string checks
    maxLength = (Number) jsonObject.get("maxLength");
    minLength = (Number) jsonObject.get("minLength");
    Object patternObject = jsonObject.get("pattern");

    String _pattern = null;
    if (patternObject != null) {
      _pattern = (String) patternObject;
    }
    pattern = _pattern;

    Object formatObject = jsonObject.get("format");
    format = formatObject instanceof String ? (String) formatObject : null;

    Object contentEncodingObject = jsonObject.get("contentEncoding");
    contentEncoding =
        contentEncodingObject instanceof String ? (String) contentEncodingObject : null;

    Object contentMediaTypeObject = jsonObject.get("contentMediaType");
    contentMediaType =
        contentMediaTypeObject instanceof String ? (String) contentMediaTypeObject : null;

    // array checks
    Object prefixItemsObject = jsonObject.get("prefixItems");
    URI prefixItemsPath = append(uri, "prefixItems");
    if (prefixItemsObject instanceof List) {
      prefixItems = new ArrayList<>();
      Collection<Object> jsonArray = (Collection<Object>) prefixItemsObject;
      for (int idx = 0; idx != jsonArray.size(); idx++) {
        prefixItems.add(getChildSchema(schemaStore, append(prefixItemsPath, String.valueOf(idx))));
      }
    } else {
      prefixItems = null;
    }

    additionalItems = getChildSchema(schemaStore, jsonObject, uri, "additionalItems");
    unevaluatedItems = getChildSchema(schemaStore, jsonObject, uri, "unevaluatedItems");

    Object itemsObject = jsonObject.get("items");
    URI itemsPath = append(uri, "items");
    if (itemsObject instanceof List) {
      itemsTuple = new ArrayList<>();
      Collection<Object> jsonArray = (Collection<Object>) itemsObject;
      for (int idx = 0; idx != jsonArray.size(); idx++) {
        itemsTuple.add(getChildSchema(schemaStore, append(itemsPath, String.valueOf(idx))));
      }
      _items = null;
    } else {
      itemsTuple = null;
      _items = getChildSchema(schemaStore, jsonObject, uri, "items");
    }

    maxItems = (Number) jsonObject.get("maxItems");
    minItems = (Number) jsonObject.get("minItems");
    uniqueItems = getOrDefault(jsonObject, "uniqueItems", false);
    contains = getChildSchema(schemaStore, jsonObject, uri, "contains");
    minContains = (Number) jsonObject.get("minContains");
    maxContains = (Number) jsonObject.get("maxContains");

    // object checks
    maxProperties = (Number) jsonObject.get("maxProperties");
    minProperties = (Number) jsonObject.get("minProperties");

    Object requiredObject = jsonObject.get("required");
    if (requiredObject instanceof List) {
      for (Object req : (Iterable<Object>) requiredObject) {
        requiredProperties.add((String) req);
      }
    }
    required = requiredObject instanceof Boolean && (Boolean) requiredObject;

    additionalProperties = getChildSchema(schemaStore, jsonObject, uri, "additionalProperties");
    unevaluatedProperties = getChildSchema(schemaStore, jsonObject, uri, "unevaluatedProperties");

    Object definitionsObject = jsonObject.get("definitions");
    if (definitionsObject instanceof Map) {
      Map<String, Object> definitions = (Map<String, Object>) definitionsObject;
      URI propertiesPointer = append(uri, "definitions");
      for (String key : definitions.keySet()) {
        getChildSchema(schemaStore, append(propertiesPointer, key));
      }
    }

    Object defsObject = jsonObject.get("$defs");
    if (defsObject instanceof Map) {
      Map<String, Object> defs = (Map<String, Object>) defsObject;
      URI propertiesPointer = append(uri, "$defs");
      for (String key : defs.keySet()) {
        getChildSchema(schemaStore, append(propertiesPointer, key));
      }
    }

    Object propertiesObject = jsonObject.get("properties");
    if (propertiesObject instanceof Map) {
      Map<String, Object> properties = (Map<String, Object>) propertiesObject;
      URI propertiesPointer = append(uri, "properties");
      for (String propertyName : properties.keySet()) {
        URI propertyUri = append(propertiesPointer, propertyName);
        _properties.put(propertyName, getChildSchema(schemaStore, propertyUri));
      }
    }

    Object patternPropertiesObject = jsonObject.get("patternProperties");
    if (patternPropertiesObject instanceof Map) {
      Map<String, Object> patternProperties = (Map<String, Object>) patternPropertiesObject;
      URI propertiesPointer = append(uri, "patternProperties");
      for (String propertyPattern : patternProperties.keySet()) {
        patternPropertiesPatterns.add(propertyPattern);
        URI patternPointer = append(propertiesPointer, propertyPattern);
        patternPropertiesSchemas.add(getChildSchema(schemaStore, patternPointer));
      }
    }

    Map<String, Object> dependenciesJsonObject =
        (Map<String, Object>) jsonObject.get("dependencies");
    if (dependenciesJsonObject != null) {
      for (Map.Entry<String, Object> entry : dependenciesJsonObject.entrySet()) {
        String dependency = entry.getKey();
        Collection<String> spec = new ArrayList<>();
        Object dependencyObject = entry.getValue();
        if (dependencyObject instanceof List) {
          for (Object o : (Iterable<Object>) dependencyObject) {
            spec.add((String) o);
          }
          dependentRequired.put(dependency, spec);
        } else if (dependencyObject instanceof Map || dependencyObject instanceof Boolean) {
          URI dependenciesPointer = append(uri, "dependencies");
          dependentSchemas.put(
              dependency, getChildSchema(schemaStore, append(dependenciesPointer, dependency)));
        } else {
          Collection<String> objects = new ArrayList<>();
          objects.add((String) dependencyObject);
          dependentRequired.put(dependency, objects);
        }
      }
    }

    Map<String, Object> dependentRequiredJsonObject =
        (Map<String, Object>) jsonObject.get("dependentRequired");
    if (dependentRequiredJsonObject != null) {
      for (Map.Entry<String, Object> entry : dependentRequiredJsonObject.entrySet()) {
        Collection<String> spec = new ArrayList<>();
        for (Object req : (Iterable<Object>) entry.getValue()) {
          spec.add((String) req);
        }
        dependentRequired.put(entry.getKey(), spec);
      }
    }

    Map<String, Object> dependentSchemasJsonObject =
        (Map<String, Object>) jsonObject.get("dependentSchemas");
    if (dependentSchemasJsonObject != null) {
      for (String dependency : dependentSchemasJsonObject.keySet()) {
        URI dependenciesPointer = append(uri, "dependentSchemas");
        dependentSchemas.put(
            dependency, getChildSchema(schemaStore, append(dependenciesPointer, dependency)));
      }
    }

    propertyNames = getChildSchema(schemaStore, jsonObject, uri, "propertyNames");

    // all types checks
    if (jsonObject.containsKey("const")) {
      hasConst = true;
      _const = jsonObject.get("const");
    } else {
      hasConst = false;
      _const = null;
    }

    Collection<Object> enumArray = (Collection<Object>) jsonObject.get("enum");
    if (enumArray == null) {
      enums = null;
    } else {
      enums = new ArrayList<>();
      enums.addAll(enumArray);
    }

    Object typeObject = jsonObject.get("type");
    if (typeObject instanceof List) {
      URI typePointer = append(uri, "type");
      explicitTypes = new HashSet<>();
      List<Object> array = (List<Object>) typeObject;
      for (int idx = 0; idx != array.size(); idx++) {
        Object arrayEntryObject = array.get(idx);
        if (arrayEntryObject instanceof Boolean || arrayEntryObject instanceof Map) {
          typesSchema.add(getChildSchema(schemaStore, append(typePointer, String.valueOf(idx))));
        } else {
          explicitTypes.add((String) arrayEntryObject);
        }
      }
    } else if (typeObject instanceof String) {
      explicitTypes = setOf(typeObject.toString());
    } else {
      explicitTypes = null;
    }

    _if = getChildSchema(schemaStore, jsonObject, uri, "if");
    _then = getChildSchema(schemaStore, jsonObject, uri, "then");
    _else = getChildSchema(schemaStore, jsonObject, uri, "else");

    Object allOfObject = jsonObject.get("allOf");
    if (allOfObject instanceof List) {
      Collection<Object> array = (Collection<Object>) allOfObject;
      URI arrayPath = append(uri, "allOf");
      for (int idx = 0; idx != array.size(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(getChildSchema(schemaStore, indexPointer));
      }
    }

    Object extendsObject = jsonObject.get("extends");
    if (extendsObject instanceof List) {
      URI arrayPath = append(uri, "extends");
      Collection<Object> array = (Collection<Object>) extendsObject;
      for (int idx = 0; idx != array.size(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        allOf.add(getChildSchema(schemaStore, indexPointer));
      }
    } else if (extendsObject instanceof Map || extendsObject instanceof Boolean) {
      URI arrayPath = append(uri, "extends");
      allOf.add(getChildSchema(schemaStore, arrayPath));
    }

    Object refObject = jsonObject.get("$ref");
    if (refObject instanceof String) {
      // Refs should be URL Escaped already; but in practice they are sometimes not.
      URI resolved = resolve(uri, URI.create(fixUnescaped((String) refObject)));
      ref = getSubSchema(schemaStore, resolved);
    } else {
      ref = null;
    }

    Object recursiveRefObject = jsonObject.get("$recursiveRef");
    if (recursiveRefObject instanceof String) {
      URI resolved = resolve(uri, URI.create((String) recursiveRefObject));
      recursiveRef = getSubSchema(schemaStore, resolved);
    } else {
      recursiveRef = null;
    }

    recursiveAnchor = getOrDefault(jsonObject, "$recursiveAnchor", false);

    URI schemaResource = UriUtils.withoutFragment(uri);
    Set<String> dynamicAnchorsInResource =
        schemaStore.getDynamicAnchorsForSchemaResource(schemaResource);
    if (dynamicAnchorsInResource != null) {
      for (String anchor : dynamicAnchorsInResource) {
        try {
          URI uri1 = new URI(uri.getScheme(), uri.getHost(), uri.getPath(), anchor);
          Schema schema = getSubSchema(schemaStore, uri1);
          this.dynamicAnchorsInResource.put(anchor, schema);
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }
    }

    Object dynamicRefObject = jsonObject.get("$dynamicRef");
    if (dynamicRefObject instanceof String) {
      // Refs should be URL Escaped already; but in practice they are sometimes not.
      dynamicRefURI = URI.create(fixUnescaped((String) dynamicRefObject));
      defaultDynamicRef = getSubSchema(schemaStore, resolve(uri, dynamicRefURI));
    } else {
      dynamicRefURI = null;
      defaultDynamicRef = null;
    }

    Object dynamicAnchorObject = jsonObject.get("$dynamicAnchor");
    if (dynamicAnchorObject instanceof String) {
      dynamicAnchor = (String) dynamicAnchorObject;
    } else {
      dynamicAnchor = null;
    }

    Object anyOfObject = jsonObject.get("anyOf");
    if (anyOfObject instanceof List) {
      anyOf = new ArrayList<>();
      Collection<Object> array = (Collection<Object>) anyOfObject;
      URI arrayPath = append(uri, "anyOf");
      for (int idx = 0; idx != array.size(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        anyOf.add(getChildSchema(schemaStore, indexPointer));
      }
    } else {
      anyOf = null;
    }

    Object oneOfObject = jsonObject.get("oneOf");
    if (oneOfObject instanceof List) {
      oneOf = new ArrayList<>();
      Collection<Object> array = (Collection<Object>) oneOfObject;
      URI arrayPath = append(uri, "oneOf");
      for (int idx = 0; idx != array.size(); idx++) {
        URI indexPointer = append(arrayPath, String.valueOf(idx));
        oneOf.add(getChildSchema(schemaStore, indexPointer));
      }
    } else {
      oneOf = null;
    }

    not = getChildSchema(schemaStore, jsonObject, uri, "not");

    Object disallowObject = jsonObject.get("disallow");
    if (disallowObject instanceof String) {
      disallow.add(disallowObject.toString());
    } else if (disallowObject instanceof List) {
      List<Object> array = (List<Object>) disallowObject;
      URI disallowPointer = append(uri, "disallow");
      for (int idx = 0; idx != array.size(); idx++) {
        Object disallowEntryObject = array.get(idx);
        if (disallowEntryObject instanceof String) {
          disallow.add((String) array.get(idx));
        } else {
          disallowSchemas.add(
              getChildSchema(schemaStore, append(disallowPointer, String.valueOf(idx))));
        }
      }
    }

    defaultValue = jsonObject.get("default");
    title = (String) jsonObject.get("title");
    description = (String) jsonObject.get("description");
    examples = (List<Object>) jsonObject.get("examples");
  }

  private Schema getChildSchema(SchemaStore schemaStore, Map<String, Object> jsonObject, URI uri,
      String name) throws GenerationException {
    Object childObject = jsonObject.get(name);
    if (childObject instanceof Map || childObject instanceof Boolean) {
      return getChildSchema(schemaStore, append(uri, name));
    }
    return null;
  }

  private Schema getChildSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    // A 'child' schema is a subSchema that can be a value or (a value or item of a container) in
    // the schema object *directly*. Schemas included via $ref, $dynamicRef are not child schemas.
    // The distinction is for clients that might benefit from knowledge of where a schema is
    // to be found in the network of schemas, where it can be given via 'getParent()'.
    Schema subSchema = getSubSchema(schemaStore, uri);
    if (subSchema != null && subSchema.getUri().equals(uri)) {
      subSchema.setParent(this);
    }
    return subSchema;
  }

  private Schema getSubSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    Schema subSchema = schemaStore.loadSchema(uri, null);
    if (subSchema != null) {
      subSchemas.put(subSchema.getUri(), subSchema);
    }
    return subSchema;
  }

  public Boolean isFalse() {
    return isFalse;
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

  public Object getResourceUri() {
    return resourceUri;
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

  public List<Schema> getPrefixItems() {
    return prefixItems == null ? null : Collections.unmodifiableList(prefixItems);
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

  public boolean hasConst() {
    return hasConst;
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

  public Map<String, Schema> getDynamicAnchorsInResource() {
    return unmodifiableMap(dynamicAnchorsInResource);
  }

  public URI getDynamicRefURI() {
    return dynamicRefURI;
  }

  public Schema getDefaultDynamicRef() {
    return defaultDynamicRef;
  }

  public String getDynamicAnchor() {
    return dynamicAnchor;
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

  public List<Object> getExamples() {
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
      metaSchema = detectMetaSchema(baseObject);
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

  public Map<URI, Schema> getSubSchemas() {
    return unmodifiableMap(subSchemas);
  }

  public void validateExamples(Validator validator, Consumer<ValidationError> errorConsumer) {
    if (examples == null) {
      return;
    }
    for (int idx = 0; idx != examples.size(); idx++) {
      URI resourceUri = URI.create("#" + this.resourceUri.getRawFragment());
      URI example = append(append(resourceUri, "examples"), String.valueOf(idx));
      validator.validate(this, baseObject, example, errorConsumer);
    }
  }

  public void validateExamplesRecursive(
      Validator validator, Consumer<ValidationError> errorConsumer) {
    Map<URI, Schema> unevaluated = new LinkedHashMap<>(subSchemas);
    Collection<URI> evaluated = new LinkedHashSet<>();
    while (!unevaluated.isEmpty()) {
      Map.Entry<URI, Schema> next = unevaluated.entrySet().iterator().next();
      unevaluated.remove(next.getKey());
      for (Map.Entry<URI, Schema> entry : next.getValue().getSubSchemas().entrySet()) {
        if (evaluated.contains(entry.getKey())) {
          continue;
        }
        unevaluated.put(entry.getKey(), entry.getValue());
      }
      next.getValue().validateExamples(validator, errorConsumer);
      evaluated.add(next.getKey());
    }
  }
}
