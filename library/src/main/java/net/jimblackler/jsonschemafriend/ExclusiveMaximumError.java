package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ExclusiveMaximumError extends ValidationError {
  public ExclusiveMaximumError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Greater than or equal to exclusive maximum: " + getSchema().getExclusiveMaximum();
  }
}
