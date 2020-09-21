package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinContainsError extends ValidationError {
  public MinContainsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Too few elements in the array matched contains";
  }
}
