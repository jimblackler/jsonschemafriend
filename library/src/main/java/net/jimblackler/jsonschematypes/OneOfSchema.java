package net.jimblackler.jsonschematypes;

import java.net.URI;

public class OneOfSchema extends ArrayBasedSchema {
  public OneOfSchema(SchemaStore schemaStore, URI uri) throws GenerationException {
    super(schemaStore, uri);
  }
}
