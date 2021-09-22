package net.jimblackler.jsonschemafriend;

import java.io.InputStream;
import java.net.URL;

public class ResourceUtils {
  static URL getResource(String resource) {
    return Thread.currentThread().getContextClassLoader().getResource(resource);
  }

  static InputStream getResourceAsStream(String resource) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
  }
}
