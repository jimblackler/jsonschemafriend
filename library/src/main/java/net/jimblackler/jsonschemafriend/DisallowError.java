package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class DisallowError extends ValidationError {
  public DisallowError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  public String getMessage() {
    return "Disallow condition passed";
  }
}
