package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinPropertiesError extends ValidationError {
  public MinPropertiesError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Too few properties";
  }
}
