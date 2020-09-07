package net.jimblackler.jsonschemafriend;

import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

public class ComparableMutable {
  private final Class<? extends Object> _class;
  private final String string;

  public ComparableMutable(Object a) {
    _class = a.getClass();
    string = a.toString();
  }

  static Object makeComparable(Object a) {
    if (a == null) {
      return new ComparableNull();
    } else if (a instanceof Number) {
      return ((Number) a).doubleValue();
    } else if (a instanceof JSONArray || a instanceof JSONObject) {
      return new ComparableMutable(a);
    }
    return a;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ComparableMutable other = (ComparableMutable) obj;
    return string.equals(other.string) && _class.equals(other._class);
  }

  @Override
  public int hashCode() {
    return Objects.hash(string, _class);
  }
}
