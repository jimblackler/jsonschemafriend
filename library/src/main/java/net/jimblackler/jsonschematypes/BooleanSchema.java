package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class BooleanSchema extends Schema {
  private final boolean value;

  BooleanSchema(SchemaStore schemaStore, URI path, boolean value) throws GenerationException {
    super(schemaStore, path);
    this.value = value;
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {
    if (!value) {
      errorConsumer.accept(new ValidationError("Boolean schema was false"));
    }
  }
}
