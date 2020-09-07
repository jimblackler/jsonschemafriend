package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MultipleError extends ValidationError {
  public MultipleError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Not a multiple of " + getSchema().getMultipleOf();
  }
}
