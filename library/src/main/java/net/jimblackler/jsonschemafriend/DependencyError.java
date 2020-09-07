package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class DependencyError extends ValidationError {
  public DependencyError(
      URI uri, Object document, String property, String dependency, Schema schema) {
    super(uri, document, "Missing dependency " + property + " -> " + dependency, schema);
  }
}
