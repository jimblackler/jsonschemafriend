package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinItemsError extends ValidationError {
  public MinItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, "Below min items: " + schema.getMinItems(), schema);
  }
}
