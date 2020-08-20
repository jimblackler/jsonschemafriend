package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public class TrivialSchema extends Schema {
  private final boolean value;

  TrivialSchema(SchemaStore schemaStore, URI path, boolean value) throws GenerationException {
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
