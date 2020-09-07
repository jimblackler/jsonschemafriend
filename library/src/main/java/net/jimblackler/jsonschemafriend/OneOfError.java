package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.List;

public class OneOfError extends ValidationError {
  private final List<List<ValidationError>> allErrors;
  private final int numberPassed;

  public OneOfError(URI uri, Object document, int numberPassed,
      List<List<ValidationError>> allErrors, Schema schema) {
    super(uri, document, "oneOf: " + numberPassed + " passed, not 1", schema);
    this.numberPassed = numberPassed;
    this.allErrors = allErrors;
  }

  public List<List<ValidationError>> getAllErrors() {
    return allErrors;
  }

  public int getNumberPassed() {
    return numberPassed;
  }
}
