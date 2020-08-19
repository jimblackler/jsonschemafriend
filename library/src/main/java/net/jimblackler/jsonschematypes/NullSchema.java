package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class NullSchema implements Schema {
  public NullSchema(SchemaStore schemaStore, URI uri) throws GenerationException {}

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
