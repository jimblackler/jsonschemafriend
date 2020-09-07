package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaximumError extends ValidationError {
  public MaximumError(URI uri, Object document, Schema schema) {
    super(uri, document,
        "Greater than maximum: " + schema.getMaximum()
            + (schema.getExclusiveMaximumBoolean() ? " (exclusive)" : ""),
        schema);
  }
}
