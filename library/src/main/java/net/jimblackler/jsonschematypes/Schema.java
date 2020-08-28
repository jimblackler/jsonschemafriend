package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Schema {
  private final URI uri;

  Schema(SchemaStore schemaStore, URI uri) throws GenerationException {
    // The schema is registered here to allow for circular graphs to be built in the constructors.
    this.uri = uri;
    schemaStore.register(uri, this);
  }

  public URI getUri() {
    return uri;
  }

  @Override
  public String toString() {
    return uri.toString();
  }

  ValidationError error(Object document, URI path, String message) {
    return new ValidationError(path, document, message, this);
  }

  abstract void validate(Object document, URI path, Consumer<ValidationError> errorConsumer);

  public abstract Map<String, Schema> getProperties();
}
