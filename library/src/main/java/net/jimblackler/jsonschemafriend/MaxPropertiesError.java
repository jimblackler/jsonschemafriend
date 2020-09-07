package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MaxPropertiesError extends ValidationError {
  public MaxPropertiesError(URI uri, Object document, Schema schema) {
    super(uri, document, "Too many properties", schema);
  }
}
