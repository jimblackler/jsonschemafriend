package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ExclusiveMinimumError extends ValidationError {
  public ExclusiveMinimumError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Less than or equal to exclusive minimum: " + getSchema().getExclusiveMinimum();
  }
}
