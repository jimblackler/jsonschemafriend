package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class EnumError extends ValidationError {
  public EnumError(URI uri, Object document, Schema schema) {
    super(uri, document, schema);
  }

  @Override
  String getMessage() {
    return "Object not in enums: " + getSchema().getEnums();
  }
}
