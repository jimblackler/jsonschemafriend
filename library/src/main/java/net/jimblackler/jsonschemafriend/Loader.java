package net.jimblackler.jsonschemafriend;

import java.io.IOException;
import java.net.URI;

public interface Loader {
  public String load(URI uri, boolean cacheSchema) throws IOException;
}
