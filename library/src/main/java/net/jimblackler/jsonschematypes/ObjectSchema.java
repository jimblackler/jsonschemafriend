package net.jimblackler.jsonschematypes;

import java.util.Iterator;
import org.json.JSONObject;

public class ObjectSchema extends SchemaWithContext {
  public ObjectSchema(SchemaContext context, JSONObject jsonObject) throws GenerationException {
    super(context);
    { // Properties
      JSONObject properties = jsonObject.getJSONObject("properties");
      JSONObject aDefault = properties.optJSONObject("default");
      // https://tools.ietf.org/html/draft-handrews-json-schema-02#section-9.3.2.3
      Object additonalPropertiesJson = properties.opt("additionalProperties");
      if (additonalPropertiesJson != null) {
        Schema additionalPropertiesSchemea = Schemas.create(null, additonalPropertiesJson);
        // We're not doing anything with this yet.
      }

      Iterator<String> it = properties.keys();
      while (it.hasNext()) {
        String propertyName = it.next();
        JSONObject propertySchema = properties.getJSONObject(propertyName);
        Schemas.create(new PropertySchemaContext(propertyName, this), propertySchema);
      }
    }
  }
}
