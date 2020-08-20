package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.function.Consumer;

public abstract class Schema {
  Schema(SchemaStore schemaStore, URI path) throws GenerationException {
    // The schema is registered here to allow for circular graphs to be built in the constructors.
    schemaStore.register(path, this);
  }
  abstract void validate(Object jsonObject, Consumer<ValidationError> errorConsumer);
}
