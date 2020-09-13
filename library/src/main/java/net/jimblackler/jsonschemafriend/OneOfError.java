package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class OneOfError extends ValidationError {
  private final List<List<ValidationError>> allErrors;
  private final List<Schema> passed;

  public OneOfError(URI uri, Object document, List<Schema> passed,
                    List<List<ValidationError>> allErrors, Schema schema) {
    super(uri, document, schema);
    this.passed = passed;
    this.allErrors = allErrors;
  }

  public List<List<ValidationError>> getAllErrors() {
    return allErrors;
  }

  public List<Schema> getPassed() {
    return passed;
  }

  @Override
  String getMessage() {
    if (passed.isEmpty()) {
      return "No oneOf passed. Errors were: " + allErrors;
    }

    return "More than one oneOf passed: " + passed.stream()
        .map(schema -> schema.getUri().toString())
        .collect(Collectors.joining(", "));

  }
}
