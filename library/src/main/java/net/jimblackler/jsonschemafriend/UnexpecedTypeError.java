package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UnexpecedTypeError extends ValidationError {
  public UnexpecedTypeError(URI uri, Object document, Object object, Schema schema) {
    super(uri, document, "Unexpected type in data: " + object.getClass().getSimpleName(), schema);
  }
}
