package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UniqueItemsError extends ValidationError {
  public UniqueItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, "Items were not unique", schema);
  }
}
