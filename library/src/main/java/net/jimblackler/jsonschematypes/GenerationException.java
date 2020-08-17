package net.jimblackler.jsonschematypes;

public class GenerationException extends Exception {
  public GenerationException() {}

  public GenerationException(String message) {
    super(message);
  }

  public GenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public GenerationException(Throwable cause) {
    super(cause);
  }

  public GenerationException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
