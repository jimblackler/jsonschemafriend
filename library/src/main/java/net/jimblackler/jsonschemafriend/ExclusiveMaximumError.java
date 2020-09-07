package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ExclusiveMaximumError extends ValidationError {
  public ExclusiveMaximumError(URI uri, Object document, Schema schema) {
    super(uri, document,
        "Greater than or equal to exclusive maximum: " + schema.getExclusiveMaximum(), schema);
  }
}
