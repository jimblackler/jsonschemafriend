package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class BooleanSchema implements Schema {
  public BooleanSchema(SchemaStore schemaStore, URI uri) throws GenerationException {}

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
