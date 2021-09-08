package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UniqueItemsError extends ValidationError {
  public UniqueItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  public String getMessage() {
    return "Items were not unique";
  }
}
