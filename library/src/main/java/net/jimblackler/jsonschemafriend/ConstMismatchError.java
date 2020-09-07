package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ConstMismatchError extends ValidationError {

  public ConstMismatchError(URI uri, Object document, Schema schema) {
    super(uri, document, "Expected const: " + schema.getConst(), schema);
  }

}
