package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinLengthError extends ValidationError {
  public MinLengthError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Shorter than minLength: " + getSchema().getMinLength();
  }
}
