package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.List;

public class AnyOfError extends ValidationError {
  private final List<List<ValidationError>> allErrors;

  public AnyOfError(
      URI uri, Object document, List<List<ValidationError>> allErrors, Schema schema) {
    super(uri, document, "All anyOf failed", schema);
    this.allErrors = allErrors;
  }

  public List<List<ValidationError>> getAllErrors() {
    return allErrors;
  }
}
