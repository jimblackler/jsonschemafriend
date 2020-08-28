package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public class BooleanSchema extends Schema {
  private final boolean value;

  BooleanSchema(SchemaStore schemaStore, URI path, boolean value) throws GenerationException {
    super(schemaStore, path);
    this.value = value;
  }

  @Override
  public void validate(Object document, URI path, Consumer<ValidationError> errorConsumer) {
    if (!value) {
      errorConsumer.accept(error(document, path, "Boolean schema was false"));
    }
  }

  @Override
  public Map<String, Schema> getProperties() {
    return null;
  }
}
