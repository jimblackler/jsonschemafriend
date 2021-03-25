package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class UnexpecedTypeError extends ValidationError {
  private final Object object;
  private final URI uri;

  public UnexpecedTypeError(URI uri, Object document, Object object, Schema schema) {
    super(uri, document, schema);
    this.uri = uri;
    this.object = object;
  }

  @Override
  String getMessage() {
    if (uri.toString().isEmpty()) {
      return "Unexpected type in data: " + object.getClass().getSimpleName();
    }
    return "Unexpected type in data at " + uri + " : " + object.getClass().getSimpleName();
  }
}
