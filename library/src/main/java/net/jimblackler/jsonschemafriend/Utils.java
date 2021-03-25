package net.jimblackler.jsonschemafriend;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Utils {
  static <T> Set<T> setOf(T element) {
    // Set.of() is only available in Java 9+. We try to keep the libary as compatible as possible.
    Set<T> set = new HashSet<>();
    set.add(element);
    return set;
  }

  static <T> T getOrDefault(Map<String, Object> map, String key, T def) {
    return map.containsKey(key) ? (T) map.get(key) : def;
  }
}
