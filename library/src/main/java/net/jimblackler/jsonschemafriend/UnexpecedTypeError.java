package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UnexpecedTypeError extends ValidationError {
  private final Object object;

  public UnexpecedTypeError(URI uri, Object document, Object object, Schema schema) {
    super(uri, document, schema);
    this.object = object;
  }

  @Override
  String getMessage() {
    return "Unexpected type in data: " + object.getClass().getSimpleName();
  }
}
