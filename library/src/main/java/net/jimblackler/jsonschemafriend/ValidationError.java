package net.jimblackler.jsonschemafriend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;

public abstract class ValidationError {
  private static final String ELLIPSES = "...";
  private final URI uri;
  private final Object document;
  private final Schema schema;
  private final Object object;
  private final ObjectMapper objectMapper = new ObjectMapper();

  protected ValidationError(URI uri, Object document, Schema schema) {
    this.uri = uri;
    this.document = document;
    this.schema = schema;
    Object _object;
    try {
      _object = Validator.getObject(document, uri);
    } catch (MissingPathException e) {
      _object = null;
    }
    object = _object;
  }

  @Override
  public String toString() {
    URI schemaPath = schema.getUri();
    try {
      String str = objectMapper.writeValueAsString(object);
      return (truncate(str, 60) + (uri.toString().isEmpty() ? " at root " : " at " + uri + " ")
          + "failed " + (schemaPath.toString().isEmpty() ? "" : "against " + schemaPath + " ")
          + "with \"" + getMessage() + "\"");
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Object getObject() {
    return object;
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

  public abstract String getMessage();

  public static String truncate(String str, int maxLength) {
    int trueMax = maxLength - ELLIPSES.length();
    if (str.length() <= trueMax) {
      return str;
    }
    return str.substring(0, trueMax) + ELLIPSES;
  }
}
