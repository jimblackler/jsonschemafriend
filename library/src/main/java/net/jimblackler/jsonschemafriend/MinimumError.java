package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinimumError extends ValidationError {
  public MinimumError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  public String getMessage() {
    return "Less than minimum: " + getSchema().getMinimum()
        + (getSchema().isExclusiveMinimumBoolean() ? " (exclusive)" : "");
  }
}
