package net.jimblackler.jsonschematypes.codegen;

import java.util.Arrays;
import java.util.stream.Collectors;
import net.jimblackler.jsonschemafriend.Schema;

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

  public static String camelToSnake(String in) {
    return in.replaceAll("([^_A-Z])([A-Z])", "$1_$2");
  }

  static String nameForSchema(Schema schema) {
    String[] split = schema.getUri().toString().split("/");
    String lastPart = split[split.length - 1];
    String namePart = lastPart.split("\\.", 2)[0];

    String name = makeJavaStyleIdentifier(namePart);
    Schema parentSchema = schema.getParent();
    if (parentSchema != null
        && (parentSchema.getItems() == schema || parentSchema.getAdditionalItems() == schema
            || (parentSchema.getItemsTuple() != null
                && parentSchema.getItemsTuple().contains(schema)))) {
      if ("items".equals(name)) {
        String parentName = nameForSchema(parentSchema);
        if (parentName.endsWith("s")) {
          name = parentName;
        }
      }

      if (schema.getParent() != null && name.endsWith("s")) {
        name = name.substring(0, name.length() - "s".length());
      }
    }
    return name;
  }

  static String makeJavaStyleIdentifier(String namePart) {
    String converted = snakeToCamel(namePart);
    return makeJavaLegal(converted);
  }

  static String makeJavaLegalPackage(String _package) {
    return Arrays.stream(_package.split("\\."))
        .map(NameUtils::makeJavaLegal)
        .collect(Collectors.joining("."));
  }

  static String makeJavaLegal(String converted) {
    if (converted.isEmpty() || !Character.isJavaIdentifierStart(converted.charAt(0))) {
      converted = "_" + converted;
    }

    StringBuilder builder = new StringBuilder();
    for (int idx = 0; idx != converted.length(); idx++) {
      char chr = converted.charAt(idx);
      if (Character.isJavaIdentifierPart(chr)) {
        builder.append(chr);
      }
    }
    return builder.toString();
  }
}
