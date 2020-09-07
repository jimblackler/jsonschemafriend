package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MinItemsError extends ValidationError {
  public MinItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Below min items: " + getSchema().getMinItems();
  }
}
