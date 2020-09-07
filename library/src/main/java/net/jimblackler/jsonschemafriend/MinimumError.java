package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinimumError extends ValidationError {
  public MinimumError(URI uri, Object document, Schema schema) {
    super(uri, document,
        "Less than minimum: " + schema.getMinimum()
            + (schema.getExclusiveMinimumBoolean() ? " (exclusive)" : ""),
        schema);
  }
}
