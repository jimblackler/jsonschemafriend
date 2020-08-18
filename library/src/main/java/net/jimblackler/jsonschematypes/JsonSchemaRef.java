package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class JsonSchemaRef {
  public static URI append(URI jsonSchemaRef, String value) {
    // TODO: implement escaping in https://tools.ietf.org/html/rfc6901#section-3

    try {
      return new URI(null, null, null, -1, jsonSchemaRef.getPath(), null,
          Path.of(jsonSchemaRef.getFragment()).resolve(value).toString());
    } catch (URISyntaxException e) {
      throw new IllegalStateException(e);
    }
  }
}
