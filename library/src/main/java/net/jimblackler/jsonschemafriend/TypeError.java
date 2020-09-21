package net.jimblackler.jsonschemafriend;

import static java.util.Collections.unmodifiableCollection;

import java.net.URI;
import java.util.Collection;

public class TypeError extends ValidationError {
  private final Collection<String> expectedTypes;
  private final Collection<String> foundTypes;

  public TypeError(URI path, Object document, Collection<String> expected, Collection<String> found,
      Schema schema) {
    super(path, document, schema);

    expectedTypes = unmodifiableCollection(expected);
    foundTypes = unmodifiableCollection(found);
  }

  public Collection<String> getExpectedTypes() {
    return expectedTypes;
  }

  public Collection<String> getFoundTypes() {
    return foundTypes;
  }

  @Override
  String getMessage() {
    return "Expected: [" + String.join(", ", expectedTypes) + "] "
        + "Found: [" + String.join(", ", foundTypes) + "]";
  }
}
