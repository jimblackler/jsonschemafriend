package net.jimblackler.jsonschematypes;

public class UncheckedGenerationException extends RuntimeException {
  public UncheckedGenerationException() {}

  public UncheckedGenerationException(String message) {
    super(message);
  }

  public UncheckedGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public UncheckedGenerationException(Throwable cause) {
    super(cause);
  }

  public UncheckedGenerationException(
      String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
