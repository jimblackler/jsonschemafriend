package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.TypeInferrer.inferTypes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class CombinedSchema {
  private final Schema schema;

  public CombinedSchema(Schema schema) {
    this.schema = schema;
  }

  public Collection<String> getInferredTypes() {
    Collection<String> allTypes = inferTypes(schema);

    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      // For the anyOf operator we return the intersection of the types (implied or explicit) of
      // each subschema.
      for (Schema subSchema : anyOf) {
        allTypes.addAll(inferTypes(subSchema));
      }
    }

    Collection<Schema> allOf = schema.getAllOf();
    if (!allOf.isEmpty()) {
      // For the allOf operator we return the union of the types (implied or explicit) of each
      // subschema.
      for (Schema subSchema : allOf) {
        Collection<String> types = inferTypes(subSchema);
        allTypes.retainAll(types);
      }
    }

    return allTypes;
  }

  public Map<String, Schema> getProperties() {
    Map<String, Schema> allProperties = new HashMap<>(schema.getProperties());

    Collection<Schema> anyOf = schema.getAnyOf();
    if (anyOf != null && !anyOf.isEmpty()) {
      for (Schema subSchema : anyOf) {
        allProperties.putAll(subSchema.getProperties());
      }
    }

    Collection<Schema> allOf = schema.getAllOf();
    if (!allOf.isEmpty()) {
      for (Schema subSchema : allOf) {
        allProperties.putAll(subSchema.getProperties());
      }
    }

    return allProperties;
  }
}
