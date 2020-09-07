package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class NotValidationError extends ValidationError {
  public NotValidationError(URI uri, Object document, Schema schema) {
    super(uri, document, "Not condition passed", schema);
  }
}
