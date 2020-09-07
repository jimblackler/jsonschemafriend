package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class EnumError extends ValidationError {
  public EnumError(URI uri, Object document, Schema schema) {
    super(uri, document, "Object not in enums: " + schema.getEnums(), schema);
  }
}
