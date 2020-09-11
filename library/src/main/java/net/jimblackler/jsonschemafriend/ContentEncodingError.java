package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ContentEncodingError extends ValidationError {
  private final String reason;

  public ContentEncodingError(URI uri, Object document, Schema schema, String reason) {
    super(uri, document, schema);
    this.reason = reason;
  }

  @Override
  String getMessage() {
    return "Content encoding failed on: " + getSchema().getContentEncoding()
        + ". Reason: " + reason;
  }
}
