package net.jimblackler.jsonschematypes;

import java.net.URI;

public class ValidationError {
  private final URI uri;
  private final Object document;
  private final Schema schema;
  private final String message;

  public ValidationError(URI uri, Object document, String message, Schema schema) {
    this.uri = uri;
    this.document = document;
    this.schema = schema;
    this.message = message;
  }

  @Override
  public String toString() {
    URI schemaPath = schema.getUri();
    String s = PathUtils.fetchFromPath(document, uri.getRawFragment()).toString();
    return (s.length() <= 20 ? "\"" + s + "\" " : "")
        + (uri.toString().isEmpty() ? "" : "at " + uri + " ") + "failed "
        + (schemaPath.toString().isEmpty() ? "" : "against " + schemaPath + " ")
        + (schema instanceof BooleanSchema ? "" : "with \"" + message + "\"");
  }
}
