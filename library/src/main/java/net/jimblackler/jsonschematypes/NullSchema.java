package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class NullSchema extends Schema {
  public NullSchema(SchemaStore schemaStore, URI path) throws GenerationException {
    super(schemaStore, path);
  }

  @Override
  public void validate(Object object, Consumer<ValidationError> errorConsumer) {}
}
