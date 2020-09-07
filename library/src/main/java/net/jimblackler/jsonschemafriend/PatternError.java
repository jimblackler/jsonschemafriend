package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class PatternError extends ValidationError {
  public PatternError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Did not match pattern: " + getSchema().getPattern();
  }
}
