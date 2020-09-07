package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class AboveMaxItemsError extends ValidationError {
  public AboveMaxItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, "Above max items: " + schema.getMaxItems(), schema);
  }
}
