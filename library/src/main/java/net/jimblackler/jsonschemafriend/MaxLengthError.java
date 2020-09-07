package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaxLengthError extends ValidationError {
  public MaxLengthError(URI uri, Object document, Schema schema) {
    super(uri, document, "Greater than maxLength: " + schema.getMaxLength(), schema);
  }
}
