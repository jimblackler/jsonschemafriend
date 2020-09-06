package net.jimblackler.jsonschemafriend;

import java.util.HashSet;
import java.util.Set;

public class Utils {
  static Set<String> setOf(String string) {
    // Set.of() is only available in Java 9+. We try to keep the libary as compatible as possible.
    Set<String> set = new HashSet<>();
    set.add(string);
    return set;
  }
}
