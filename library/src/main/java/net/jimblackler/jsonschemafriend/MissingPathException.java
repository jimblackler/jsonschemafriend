package net.jimblackler.jsonschemafriend;

public class MissingPathException extends SchemaException {
  public MissingPathException() {
    super();
  }

  public MissingPathException(String message) {
    super(message);
  }

  public MissingPathException(String message, Throwable cause) {
    super(message, cause);
  }

  public MissingPathException(Throwable cause) {
    super(cause);
  }

  protected MissingPathException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
