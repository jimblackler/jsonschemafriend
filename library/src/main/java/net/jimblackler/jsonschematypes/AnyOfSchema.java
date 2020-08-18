package net.jimblackler.jsonschematypes;

import java.net.URI;

public class AnyOfSchema extends ArrayBasedSchema {
  public AnyOfSchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    super(schemaStore, pointer);
  }
}
