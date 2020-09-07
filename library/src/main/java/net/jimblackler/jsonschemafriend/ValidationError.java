package net.jimblackler.jsonschemafriend;

import java.net.URI;

public abstract class ValidationError {
  private final URI uri;
  private final Object document;
  private final Schema schema;

  protected ValidationError(URI uri, Object document, Schema schema) {
    this.uri = uri;
    this.document = document;
    this.schema = schema;
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
        + getMessage() + "\"";
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

  abstract String getMessage();
}
