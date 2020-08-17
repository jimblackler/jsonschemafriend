package net.jimblackler.jsonschematypes;

public class TrivialSchema implements Schema {
  private final boolean value;

  TrivialSchema(boolean value) {
    this.value = value;
  }
}
