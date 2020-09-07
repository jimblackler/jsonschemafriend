package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class GreaterThanMaximumError extends ValidationError {
  private final Number maximum;
  private final boolean exclusive;

  public GreaterThanMaximumError(
      URI uri, Object document, boolean exclusive, Number maximum, Schema schema) {
    super(uri, document, "Greater than maximum: " + maximum + (exclusive ? " (exclusive)" : ""),
        schema);
    this.maximum = maximum;
    this.exclusive = exclusive;
  }

  public Number getMaximum() {
    return maximum;
  }

  public boolean getExclusive() {
    return exclusive;
  }
}
