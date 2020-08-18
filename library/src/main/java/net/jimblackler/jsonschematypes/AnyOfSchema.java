package net.jimblackler.jsonschematypes;

import java.net.URI;

public class AnyOfSchema extends ArrayBasedSchema {
  public AnyOfSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    super(schemaStore, uri);
  }
}
