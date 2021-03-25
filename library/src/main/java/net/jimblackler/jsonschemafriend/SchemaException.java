package net.jimblackler.jsonschemafriend;

public abstract class SchemaException extends Exception {
  protected SchemaException() {}

  protected SchemaException(String message) {
    super(message);
  }

  protected SchemaException(String message, Throwable cause) {
    super(message, cause);
  }

  protected SchemaException(Throwable cause) {
    super(cause);
  }
}
