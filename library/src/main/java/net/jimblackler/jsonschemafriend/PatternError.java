package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class PatternError extends ValidationError {
  public PatternError(URI uri, Object document, Schema schema) {
    super(uri, document, "Did not match pattern: " + schema.getPattern(), schema);
  }
}
