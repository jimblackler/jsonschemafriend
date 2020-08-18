package net.jimblackler.jsonschematypes;

import java.net.URI;

public class OneOfSchema extends ArrayBasedSchema {
  public OneOfSchema(SchemaStore schemaStore, URI pointer) throws GenerationException {
    super(schemaStore, pointer);
  }
}
