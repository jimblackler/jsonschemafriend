package net.jimblackler.jsonschematypes;

import org.json.JSONArray;
import org.json.JSONObject;

public class ArraySchema extends SchemaWithContext {
  private final Schema arraySchema;

  public ArraySchema(SchemaContext schemaContext, JSONObject jsonObject)
      throws GenerationException {
    super(schemaContext);

    // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.1.1
    Object items = jsonObject.get("items");
    if (items instanceof JSONArray) {
      // Same-position validation not yet supported.
      arraySchema = null;
    } else {
      arraySchema = Schemas.create(new ArrayTypeContext(this), items);
    }
  }
}
