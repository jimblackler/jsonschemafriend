package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

/**
 * A schema defined by a boolean. "Boolean" refers to the type in the definition, not the type of
 * data it validates.
 */
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
  public boolean isObjectSchema() {
    return false;
  }

  @Override
  public ObjectSchema asObjectSchema() {
    throw new IllegalStateException("Not an object schema");
  }
}
