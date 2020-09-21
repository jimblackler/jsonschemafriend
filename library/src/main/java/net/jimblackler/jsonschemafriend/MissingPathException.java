package net.jimblackler.jsonschemafriend;

public class MissingPathException extends SchemaException {
  public MissingPathException() {}

  public MissingPathException(String message) {
    super(message);
  }

  public MissingPathException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingPathException(Throwable cause) {
    super(cause);
  }
}
