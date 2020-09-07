package net.jimblackler.jsonschemafriend;

import java.net.URI;

public abstract class ValidationError {
  private final URI uri;
  private final Object document;
  private final Schema schema;
  private final String message; // TODO: maybe not keep 'message' as a field.

  public ValidationError(URI uri, Object document, String message, Schema schema) {
    this.uri = uri;
    this.document = document;
    this.schema = schema;
    this.message = message;
  }

  @Override
  public String toString() {
    URI schemaPath = schema.getUri();
    Object object = null;
    try {
      object = PathUtils.fetchFromPath(document, uri.getRawFragment());
    } catch (MissingPathException e) {
      // Ingored by design.
    }
    String string = object == null ? "" : object.toString();
    return (string.length() <= 20 ? "\"" + string + "\" " : "")
        + (uri.toString().isEmpty() ? "" : "at " + uri + " ") + "failed "
        + (schemaPath.toString().isEmpty() ? "" : "against " + schemaPath + " ") + "with \""
        + message + "\"";
  }

  public URI getUri() {
    return uri;
  }

  public Object getDocument() {
    return document;
  }

  public Schema getSchema() {
    return schema;
  }
}
