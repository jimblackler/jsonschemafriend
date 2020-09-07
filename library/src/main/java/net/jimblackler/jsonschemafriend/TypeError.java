package net.jimblackler.jsonschemafriend;

import java.net.URI;
import java.util.Collection;

public class TypeError extends ValidationError {
  private final Collection<String> expectedTypes;
  private final Collection<String> foundTypes;

  public TypeError(URI path, Object document, Collection<String> expected, Collection<String> found,
      Schema schema) {
    super(path, document,
        "Expected: [" + String.join(", ", expected) + "] "
            + "Found: [" + String.join(", ", found) + "]",
        schema);

    expectedTypes = expected;
    foundTypes = found;
  }

  public Collection<String> getExpectedTypes() {
    return expectedTypes;
  }

  public Collection<String> getFoundTypes() {
    return foundTypes;
  }
}
