package net.jimblackler.jsonschematypes;

public class SchemaWithContext implements Schema {
  private final SchemaContext context;

  SchemaWithContext(SchemaContext context) {
    this.context = context;
  }
}
