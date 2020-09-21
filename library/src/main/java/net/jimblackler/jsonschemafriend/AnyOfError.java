package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class AnyOfError extends ValidationError {
  private final List<List<ValidationError>> allErrors;

  public AnyOfError(
      URI uri, Object document, List<List<ValidationError>> allErrors, Schema schema) {
    super(uri, document, schema);
    this.allErrors = allErrors;
  }

  public List<List<ValidationError>> getAllErrors() {
    return Collections.unmodifiableList(allErrors);
  }

  @Override
  String getMessage() {
    return "All anyOf failed";
  }
}
