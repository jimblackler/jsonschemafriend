package net.jimblackler.jsonschematypes;

import java.net.URI;

public class AllOfSchema extends ArrayBasedSchema {
  public AllOfSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    super(schemaStore, uri);
  }
}
