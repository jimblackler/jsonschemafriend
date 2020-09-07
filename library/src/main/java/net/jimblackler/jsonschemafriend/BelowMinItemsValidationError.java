package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class BelowMinItemsValidationError extends ValidationError {
  public BelowMinItemsValidationError(URI uri, Object document, Number minItems, Schema schema) {
    super(uri, document, "Below min items: " + minItems, schema);
  }
}
