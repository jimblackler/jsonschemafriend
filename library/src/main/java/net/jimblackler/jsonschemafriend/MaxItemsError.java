package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaxItemsError extends ValidationError {
  public MaxItemsError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  public String getMessage() {
    return "Above max items: " + getSchema().getMaxItems();
  }
}
