package net.jimblackler.jsonschematypes;

public class GenerationUncheckedException extends RuntimeException {
  public GenerationUncheckedException() {
  }

  public GenerationUncheckedException(String message) {
    super(message);
  }

  public GenerationUncheckedException(String message, Throwable cause) {
    super(message, cause);
  }

  public GenerationUncheckedException(Throwable cause) {
    super(cause);
  }

  public GenerationUncheckedException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
