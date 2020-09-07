package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class GreaterThanMaximumError extends ValidationError {

  public GreaterThanMaximumError(
      URI uri, Object document, Schema schema) {
    super(uri, document, "Greater than maximum: " + schema.getMaximum() + (schema.getExclusiveMaximumBoolean() ? " (exclusive)" : ""),
        schema);
  }

}
