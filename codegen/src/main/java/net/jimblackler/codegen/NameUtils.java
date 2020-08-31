package net.jimblackler.codegen;

public class NameUtils {
  static String capitalizeFirst(String in) {
    return Character.toUpperCase(in.charAt(0)) + in.substring(1);
  }

  static String lowerCaseFirst(String in) {
    in = in.replace("JSON", "Json");
    return Character.toLowerCase(in.charAt(0)) + in.substring(1);
  }
}
