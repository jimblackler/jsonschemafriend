package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class NumberSchema implements Schema {
  public NumberSchema(SchemaStore schemaStore, URI uri) throws GenerationException {}

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
