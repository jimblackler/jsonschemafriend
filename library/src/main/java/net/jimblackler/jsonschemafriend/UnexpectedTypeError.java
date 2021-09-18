package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UnexpectedTypeError extends ValidationError {
  private final Object object;

  public UnexpectedTypeError(URI uri, Object document, Object object, Schema schema) {
    super(uri, document, schema);
    this.object = object;
  }

  @Override
  public String getMessage() {
    return "Unexpected Java type in the document: " + object.getClass().getSimpleName();
  }
}
