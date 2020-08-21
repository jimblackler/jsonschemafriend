package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public abstract class Schema {
  private final URI path;

  Schema(SchemaStore schemaStore, URI path) throws GenerationException {
    // The schema is registered here to allow for circular graphs to be built in the constructors.
    this.path = path;
    schemaStore.register(path, this);
  }

  URI getPath() {
    return path;
  }

  @Override
  public String toString() {
    return path.toString();
  }

  ValidationError error(Object document, URI path, String message) {
    return new ValidationError(path, document, message, this);
  }

  abstract void validate(Object document, URI path, Consumer<ValidationError> errorConsumer);
}
