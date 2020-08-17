package net.jimblackler.jsonschematypes;

public class BaseSchemaContext extends SchemaContext {
  private final String schemaFilename;

  public BaseSchemaContext(String schemaFilename) {
    this.schemaFilename = schemaFilename;
  }
}
