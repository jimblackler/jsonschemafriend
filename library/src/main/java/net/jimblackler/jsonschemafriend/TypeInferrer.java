package net.jimblackler.jsonschemafriend;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class TypeInferrer {
  static String javaToSchemaType(Object object) {
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

  static Collection<String> allTypes() {
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

  public static Collection<String> inferTypes(Schema schema) {
    Collection<String> inferredTypes = new HashSet<>();

    Collection<String> explicitTypes = schema.getExplicitTypes();
    if (explicitTypes != null) {
      inferredTypes.addAll(explicitTypes);
      return inferredTypes;
    }

    List<Object> enums = schema.getEnums();
    if (enums != null) {
      for (Object enumObject : enums) {
        inferredTypes.add(javaToSchemaType(enumObject));
      }
    }

    if (schema.getMaxLength() != null || schema.getMinLength() != null
        || schema.getPattern() != null || schema.getFormat() != null
        || schema.getContentEncoding() != null || schema.getContentMediaType() != null) {
      inferredTypes.add("string");
    }

    if (schema.getMultipleOf() != null || schema.getMaximum() != null
        || schema.getExclusiveMaximum() != null || schema.getMinimum() != null
        || schema.getExclusiveMaximum() != null || schema.getDivisibleBy() != null) {
      inferredTypes.add("number");
    }

    if (schema.getAdditionalItems() != null || schema.getItems() != null
        || schema.getMaxItems() != null || schema.getMinItems() != null || schema.isUniqueItems()
        || schema.getContains() != null) {
      inferredTypes.add("array");
    }

    if (schema.getMaxProperties() != null || schema.getMinProperties() != null
        || !schema.getRequiredProperties().isEmpty() || schema.isRequired()
        || schema.getAdditionalProperties() != null || !schema.getProperties().isEmpty()
        || !schema.getPatternPropertiesPatterns().isEmpty()
        || !schema.getDependentSchemas().isEmpty() || schema.getPropertyNames() != null) {
      inferredTypes.add("object");
    }

    Object defaultValue = schema.getDefault();
    if (defaultValue != null) {
      inferredTypes.add(javaToSchemaType(defaultValue));
    }
    return inferredTypes;
  }

  public static Collection<String> inferTypesNonEmpty(Schema schema) {
    Collection<String> inferredTypes = inferTypes(schema);
    if (inferredTypes.isEmpty()) {
      // If type inference found nothing, we don't want to imply no types are allowed.
      return getNonProhibitedTypes(schema);
    }
    return inferredTypes;
  }

  public static Collection<String> getNonProhibitedTypes(Schema schema) {
    Collection<String> explicitTypes = schema.getExplicitTypes();
    if (explicitTypes == null) {
      return allTypes();
    }
    return explicitTypes;
  }

  public static Collection<String> getNonProhibitedTypes(CombinedSchema schema) {
    return schema.getNonProhibitedTypes();
  }
}
