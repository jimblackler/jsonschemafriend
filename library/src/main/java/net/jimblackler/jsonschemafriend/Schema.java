package net.jimblackler.jsonschemafriend;

import java.net.URI;
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

  public void validate(Object document, Consumer<ValidationError> errorConsumer) {
    validate(document, URI.create(""), errorConsumer);
  }

  public abstract void validate(Object document, URI path, Consumer<ValidationError> errorConsumer);

  // TODO: get rid of this and set defaults on Schema for methods.
  public abstract boolean isObjectSchema();

  public abstract ObjectSchema asObjectSchema();
}
