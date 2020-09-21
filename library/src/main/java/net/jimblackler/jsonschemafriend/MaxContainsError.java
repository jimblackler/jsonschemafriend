package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaxContainsError extends ValidationError {
  public MaxContainsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Too many elements in the array matched contains";
  }
}
