package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaximumError extends ValidationError {
  public MaximumError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Greater than maximum: " + getSchema().getMaximum()
        + (getSchema().isExclusiveMaximumBoolean() ? " (exclusive)" : "");
  }
}
