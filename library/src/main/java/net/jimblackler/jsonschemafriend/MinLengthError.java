package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinLengthError extends ValidationError {
  public MinLengthError(URI uri, Object document, Schema schema) {
    super(uri, document, "Shorter than minLength: " + schema.getMinLength(), schema);
  }
}
