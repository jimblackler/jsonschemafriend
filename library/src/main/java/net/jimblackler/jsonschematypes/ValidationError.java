package net.jimblackler.jsonschematypes;

public class ValidationError {
  private final String message;

  public ValidationError(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return message;
  }
}
