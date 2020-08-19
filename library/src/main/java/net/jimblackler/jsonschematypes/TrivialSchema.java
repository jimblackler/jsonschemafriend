package net.jimblackler.jsonschematypes;

import java.util.function.Consumer;

public class TrivialSchema implements Schema {
  private final boolean value;

  TrivialSchema(boolean value) {
    this.value = value;
  }

  @Override
  public void validate(Object jsonObject, Consumer<ValidationError> errorConsumer) {
    if (!value) {
      errorConsumer.accept(new ValidationError("Boolean schema was false"));
    }
  }
}
