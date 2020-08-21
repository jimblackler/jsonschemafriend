package net.jimblackler.jsonschematypes;

import java.net.URI;

public class ValidationError {
  private final URI path;
  private final Object document;
  private final Schema schema;
  private final String message;

  public ValidationError(URI path, Object document, String message, Schema schema) {
    this.path = path;
    this.document = document;
    this.schema = schema;
    this.message = message;
  }

  @Override
  public String toString() {
    URI schemaPath = schema.getPath();
    Object v = PathUtils.objectAtPath(document, path);
    String s = v.toString();
    return (s.length() <= 20 ? "\"" + s + "\" " : "") +
        (path.toString().isEmpty() ? "" : "at " + path + " ") +
        "failed " + (schemaPath.toString().isEmpty() ? "" : "against " + schemaPath + " ")
        + (schema instanceof BooleanSchema ? "" : "with \"" + message + "\"");
  }
}
