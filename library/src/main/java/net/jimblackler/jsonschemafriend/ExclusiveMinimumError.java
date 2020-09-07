package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ExclusiveMinimumError extends ValidationError {
  public ExclusiveMinimumError(URI uri, Object document, Schema schema) {
    super(uri, document, "Less than or equal to exclusive minimum: " + schema.getExclusiveMinimum(),
        schema);
  }
}
