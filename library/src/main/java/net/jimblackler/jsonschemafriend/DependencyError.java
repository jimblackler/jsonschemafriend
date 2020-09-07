package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class DependencyError extends ValidationError {
  private final String property;
  private final String dependency;

  public DependencyError(
      URI uri, Object document, String property, String dependency, Schema schema) {
    super(uri, document, schema);
    this.property = property;
    this.dependency = dependency;
  }

  @Override
  String getMessage() {
    return "Missing dependency " + property + " -> " + dependency;
  }

  public String getProperty() {
    return property;
  }

  public String getDependency() {
    return dependency;
  }
}
