package net.jimblackler.jsonschematypes;

import java.net.URI;

public class AllOfSchema extends ArrayBasedSchema {
  public AllOfSchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    super(schemaStore, pointer);
  }
}
