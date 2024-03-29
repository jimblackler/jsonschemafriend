package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class FalseSchemaError extends ValidationError {
  public FalseSchemaError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  public String getMessage() {
    return "False";
  }
}
