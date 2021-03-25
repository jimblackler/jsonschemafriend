package net.jimblackler.jsonschemafriend;

public abstract class ValidationException extends SchemaException {
  protected ValidationException(String message) {
    super(message);
  }

  protected ValidationException() {}
}
