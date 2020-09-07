package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class LessThanMinimumError extends ValidationError {
  private final Number minimum;
  private final boolean exclusive;

  public LessThanMinimumError(
      URI uri, Object document, boolean exclusive, Number minimum, Schema schema) {
    super(
        uri, document, "Less than minimum: " + minimum + (exclusive ? " (exclusive)" : ""), schema);
    this.minimum = minimum;
    this.exclusive = exclusive;
  }

  public Number getMinimum() {
    return minimum;
  }

  public boolean getExclusive() {
    return exclusive;
  }
}
