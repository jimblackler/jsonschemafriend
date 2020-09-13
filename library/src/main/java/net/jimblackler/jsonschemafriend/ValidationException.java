package net.jimblackler.jsonschemafriend;

import java.util.Collection;
import java.util.stream.Collectors;

public class ValidationException extends SchemaException {
  private final Collection<ValidationError> errors;

  public ValidationException(Collection<ValidationError> errors) {
    super("Validation errors: "
        + errors.stream().map(Object::toString).collect(Collectors.joining(", ")));
    this.errors = errors;
  }

  public Collection<ValidationError> getErrors() {
    return errors;
  }
}
