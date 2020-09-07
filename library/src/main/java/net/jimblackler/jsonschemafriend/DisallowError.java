package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class DisallowError extends ValidationError {
  public DisallowError(URI uri, Object document, Schema schema) {
    super(uri, document, "Disallow condition passed", schema);
  }
}
