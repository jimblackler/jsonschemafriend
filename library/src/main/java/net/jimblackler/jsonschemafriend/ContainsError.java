package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ContainsError extends ValidationError {
  public ContainsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "No element in the array matched contains";
  }
}
