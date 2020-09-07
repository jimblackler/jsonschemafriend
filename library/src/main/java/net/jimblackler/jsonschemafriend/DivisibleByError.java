package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class DivisibleByError extends ValidationError {
  public DivisibleByError(URI uri, Object document, Schema schema) {
    super(uri, document, "Not divisible hy " + schema.getDivisibleBy(), schema);
  }
}
