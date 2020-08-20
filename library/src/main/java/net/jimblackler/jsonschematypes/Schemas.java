package net.jimblackler.jsonschematypes;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Schemas {
  public static Schema create(SchemaStore schemaStore, URI path) throws GenerationException {
    Object object = schemaStore.resolvePath(path);
    if (object == null) {
      throw new GenerationException("Cannot follow " + path);
    }
    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-4.3.2
    if (object instanceof Boolean) {
      return new TrivialSchema(schemaStore, path, (boolean) object);
    }

    try {
      return new PrimitiveSchema(schemaStore, path);

    } catch (JSONException e) {
      throw new GenerationException(path.toString(), e);
    }
  }
}