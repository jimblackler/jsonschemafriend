package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ContentMediaTypeError extends ValidationError {
  private final String reason;

  public ContentMediaTypeError(URI uri, Object document, Schema schema, String reason) {
    super(uri, document, schema);
    this.reason = reason;
  }

  @Override
  String getMessage() {
    return "Content media type failed on: " + getSchema().getContentMediaType()
        + ". Reason: " + reason;
  }
}
