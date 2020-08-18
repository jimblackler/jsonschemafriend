package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONPointer;

public abstract class SchemaStore {
  private final Collection<URI> unbuilt = new HashSet<>();
  private final Map<URI, Schema> built = new HashMap<>();

  abstract Object load(URI uri) throws GenerationException;

  public Object resolve(URI uri) throws GenerationException {
    try {
      URI minusFragment = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
          uri.getPort(), uri.getPath(), uri.getQuery(), null);
      Object object = load(minusFragment);
      if (uri.getFragment() == null || "/".equals(uri.getFragment())) {
        return object;
      }
      JSONPointer jsonPointer = new JSONPointer("#" + uri.getFragment());
      return jsonPointer.queryFrom(object);
    } catch (URISyntaxException e) {
      throw new GenerationException(e);
    }
  }

  public URI require(URI uri) throws GenerationException {
    if (unbuilt.contains(uri) || built.containsKey(uri)) {
      return uri;
    }
    // Is a $ref?
    Object object = resolve(uri);
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      String ref = jsonObject.optString("$ref");
      if (!ref.isEmpty()) {
        try {
          return require(uri.resolve(new URI(ref)));
        } catch (URISyntaxException e) {
          throw new GenerationException(e);
        }
      }
    }
    unbuilt.add(uri);
    return uri;
  }

  public void process() throws GenerationException {
    while (!unbuilt.isEmpty()) {
      URI uri = unbuilt.iterator().next();
      System.out.println("Processing " + uri);
      built.put(uri, Schemas.create(this, uri));
      unbuilt.remove(uri);
    }
  }
}
