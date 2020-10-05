package net.jimblackler.jsonschemafriend;

abstract public class ValidationException extends SchemaException {
  public ValidationException(String message) {
    super(message);
  }

  public ValidationException() {
    super();
  }
}
