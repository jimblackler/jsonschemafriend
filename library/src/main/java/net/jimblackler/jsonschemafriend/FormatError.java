package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class FormatError extends ValidationError {
  private final String reason;

  public FormatError(URI uri, Object document, Schema schema, String reason) {
    super(uri, document, schema);
    this.reason = reason;
  }

  @Override
  String getMessage() {
    return "Not compliant with format: " + getSchema().getFormat() + ". Reason: " + reason;
  }
}
