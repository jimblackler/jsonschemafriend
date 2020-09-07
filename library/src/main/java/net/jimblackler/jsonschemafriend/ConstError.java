package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ConstError extends ValidationError {
  public ConstError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Expected const: " + getSchema().getConst();
  }
}
