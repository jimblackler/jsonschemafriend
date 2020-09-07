package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class LessThanMinimumError extends ValidationError {

  public LessThanMinimumError(
      URI uri, Object document,  Schema schema) {
    super(
        uri, document, "Less than minimum: " + schema.getMinimum() + (schema.getExclusiveMinimumBoolean() ? " (exclusive)" : ""), schema);
  }

}
