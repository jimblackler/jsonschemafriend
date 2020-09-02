package net.jimblackler.jsonschematypes.codegen;

public class NameUtils {
  static String capitalizeFirst(String in) {
    return Character.toUpperCase(in.charAt(0)) + in.substring(1);
  }

  static String lowerCaseFirst(String in) {
    return Character.toLowerCase(in.charAt(0)) + in.substring(1);
  }

  static String snakeToCamel(String in) {
    String[] parts = in.split("_");
    StringBuilder stringBuilder = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      stringBuilder.append(capitalizeFirst(part));
    }
    return stringBuilder.toString();
  }
}
