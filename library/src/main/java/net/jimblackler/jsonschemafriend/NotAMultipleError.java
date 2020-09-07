package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class NotAMultipleError extends ValidationError {
  public NotAMultipleError(URI uri, Object document, Schema schema) {
    super(uri, document, "Not a multiple of " + schema.getMultipleOf(), schema);
  }
}
