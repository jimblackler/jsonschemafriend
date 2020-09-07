package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.Collection;

public class TypeDisallowedError extends ValidationError {
  private final Collection<String> disallowed;

  public TypeDisallowedError(
      URI path, Object document, Collection<String> disallowed, Schema schema) {
    super(path, document, "Type disallowed: " + disallowed, schema);
    this.disallowed = disallowed;
  }
}
