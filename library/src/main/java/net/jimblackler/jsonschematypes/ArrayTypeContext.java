package net.jimblackler.jsonschematypes;

public class ArrayTypeContext extends SchemaContext {
  private final ArraySchema arraySchema;

  public ArrayTypeContext(ArraySchema arraySchema) {
    this.arraySchema = arraySchema;
  }
}
