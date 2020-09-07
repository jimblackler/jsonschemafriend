package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class NotError extends ValidationError {
  public NotError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Not condition passed";
  }
}
