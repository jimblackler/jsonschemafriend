package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class MissingPropertyError extends ValidationError {
  private final String property;

  public MissingPropertyError(URI uri, Object document, String property, Schema schema) {
    super(uri, document, "Missing property " + property, schema);
    this.property = property;
  }

  public String getProperty() {
    return property;
  }
}
