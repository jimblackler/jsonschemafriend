package net.jimblackler.jsonschemafriend;

public class ComparableUtils {
  static Object makeComparable(Object a) {
    if (a == null) {
      return new ComparableNull();
    } else if (a instanceof Number) {
      return ((Number) a).doubleValue();
    }
    return a;
  }
}
