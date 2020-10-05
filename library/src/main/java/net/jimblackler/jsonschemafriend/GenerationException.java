package net.jimblackler.jsonschemafriend;

public class GenerationException extends SchemaException {
  public GenerationException() {
    super();
  }

  public GenerationException(String message) {
    super(message);
  }

  public GenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public GenerationException(Throwable cause) {
    super(cause);
  }
}
