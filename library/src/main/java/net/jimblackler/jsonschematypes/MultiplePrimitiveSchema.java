package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class MultiplePrimitiveSchema implements Schema {
  private final Set<Schema> _types = new HashSet<>();

  public MultiplePrimitiveSchema(SchemaStore schemaStore, URI uri, Set<String> inTypes)
      throws GenerationException {
    Collection<String> types = new HashSet<>(inTypes);

    if (types.remove("array")) {
      _types.add(new ArraySchema(schemaStore, uri));
    }

    if (types.remove("boolean")) {
      _types.add(new BooleanSchema(schemaStore, uri));
    }

    if (types.remove("integer")) {
      _types.add(new IntegerSchema(schemaStore, uri));
    }

    if (types.remove("number")) {
      _types.add(new NumberSchema(schemaStore, uri));
    }

    if (types.remove("object")) {
      _types.add(new ObjectSchema(schemaStore, uri));
    }

    if (types.remove("string")) {
      _types.add(new StringSchema(schemaStore, uri));
    }

    if (types.remove("null")) {
      _types.add(new NullSchema(schemaStore, uri));
    }

    if (!types.isEmpty()) {
      throw new GenerationException("Unknown type: " + types);
    }
  }

  public Set<Schema> getTypes() {
    return _types;
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {

  }
}
