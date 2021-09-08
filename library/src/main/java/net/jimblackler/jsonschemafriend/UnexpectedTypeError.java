package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UnexpectedTypeError extends ValidationError {
  private final Object object;
  private final URI uri;

  public UnexpectedTypeError(URI uri, Object document, Object object, Schema schema) {
    super(uri, document, schema);
    this.uri = uri;
    this.object = object;
  }

  @Override
  public String getMessage() {
    if (uri.toString().isEmpty()) {
      return "Unexpected type in data: " + object.getClass().getSimpleName();
    }
    return "Unexpected type in data at " + uri + " : " + object.getClass().getSimpleName();
  }
}
