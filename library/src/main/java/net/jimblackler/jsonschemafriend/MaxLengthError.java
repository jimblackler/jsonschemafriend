package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaxLengthError extends ValidationError {
  public MaxLengthError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Greater than maxLength: " + getSchema().getMaxLength();
  }
}
