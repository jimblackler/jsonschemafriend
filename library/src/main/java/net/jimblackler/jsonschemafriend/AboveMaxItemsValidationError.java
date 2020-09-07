package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class AboveMaxItemsValidationError extends ValidationError {
  public AboveMaxItemsValidationError(URI uri, Object document, Number maxItems, Schema schema) {
    super(uri, document, "Above max items: " + maxItems, schema);
  }
}
