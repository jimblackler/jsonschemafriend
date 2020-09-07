package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class NotAMultipleError extends ValidationError {
  private final Number multiple;

  public NotAMultipleError(URI uri, Object document, Number multiple, Schema schema) {
    super(uri, document, "Not a multiple of " + multiple, schema);
    this.multiple = multiple;
  }

  public Number getMultiple() {
    return multiple;
  }
}
