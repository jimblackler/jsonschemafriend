package net.jimblackler.jsonschematypes;

public class PropertySchemaContext extends SchemaContext {
  private final String propertyName;
  private final Schema parent;

  public PropertySchemaContext(String propertyName, Schema parent) {
    this.propertyName = propertyName;
    this.parent = parent;
  }
}
