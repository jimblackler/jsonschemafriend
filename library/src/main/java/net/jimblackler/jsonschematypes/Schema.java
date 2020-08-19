package net.jimblackler.jsonschematypes;

import java.util.function.Consumer;

public interface Schema {
  void validate(Object jsonObject, Consumer<ValidationError> errorConsumer);
}
