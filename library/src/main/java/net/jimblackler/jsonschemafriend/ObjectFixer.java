package net.jimblackler.jsonschemafriend;

import java.math.BigInteger;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ObjectFixer {
  public static Object rewriteObject(Object object) {
    if (object instanceof JSONObject) {
      JSONObject jsonObject = (JSONObject) object;
      Iterator<String> it = jsonObject.keys();
      while (it.hasNext()) {
        String key = it.next();
        jsonObject.put(key, rewriteObject(jsonObject.get(key)));
      }
      return object;
    }
    if (object instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) object;
      for (int idx = 0; idx != jsonArray.length(); idx++) {
        jsonArray.put(idx, rewriteObject(jsonArray.get(idx)));
      }
      return object;
    }

    if (!(object instanceof String)) {
      return object;
    }
    String string = (String) object;
    // Reverse what org.json does to long numbers (converts them into strings).
    // Strings are rewritten as a number in the cases where a string would have been the only way to
    // serialize the number.
    // It would be better to have a JSON deserializer that used BigInteger where necessary.
    // But this won't be added to org.json soon: https://github.com/stleary/JSON-java/issues/157
    try {
      JSONArray testObject = new JSONArray(String.format("[%s]", string));
      if (testObject.get(0) instanceof String) {
        BigInteger bigInteger = new BigInteger(string);
        if (bigInteger.toString().equals(string)) {
          return bigInteger;
        }
      }
      return object;
    } catch (NumberFormatException | JSONException e) {
      // Doesn't look like a number after all.
      return object;
    }
  }
}
