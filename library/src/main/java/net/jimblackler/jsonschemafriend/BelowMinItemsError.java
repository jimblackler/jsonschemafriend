package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class BelowMinItemsError extends ValidationError {
  public BelowMinItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, "Below min items: " + schema.getMinItems(), schema);
  }
}
