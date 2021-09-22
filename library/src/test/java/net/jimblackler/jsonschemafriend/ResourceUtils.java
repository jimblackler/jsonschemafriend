package net.jimblackler.jsonschemafriend;

import java.io.InputStream;
import java.net.URL;

public class ResourceUtils {
  static URL getResource(Class clazz, String resource) {
    URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
    return url == null ? clazz.getResource(resource) : url;
  }

  static InputStream getResourceAsStream(Class clazz, String resource) {
    InputStream stream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    return stream == null ? clazz.getResourceAsStream(resource) : stream;
  }
}
