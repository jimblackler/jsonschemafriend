package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschematypes.codegen.NameUtils.nameForSchema;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;

public class TypeScriptBuilder {
  private final String name;
  private final Map<String, TypeScriptBuilder> fields = new HashMap<>();
  private final Collection<String> types;

  public TypeScriptBuilder(TypeScriptCodeGenerator typeScriptCodeGenerator, Schema schema) {
    typeScriptCodeGenerator.register(schema.getUri(), this);

    for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
      fields.put(entry.getKey(), typeScriptCodeGenerator.get(entry.getValue()));
    }
    types = schema.getTypes();

    if (types.size() == 1 && !types.contains("object")) {
      name = null;
      return;
    }

    name = nameForSchema(schema);
  }

  private String getTypeName() {
    if (name == null) {
      String type = types.iterator().next();
      return type;
    }
    return name;
  }

  void write(PrintWriter printWriter) {
    if (!isClass()) {
      return;
    }
    printWriter.print("class ");
    printWriter.print(name);
    printWriter.println(" {");

    for (Map.Entry<String, TypeScriptBuilder> entry : fields.entrySet()) {
      printWriter.print("  ");
      printWriter.print(entry.getKey());
      printWriter.print(": ");
      TypeScriptBuilder builder = entry.getValue();
      printWriter.print(builder.getTypeName());
      printWriter.println("; ");
    }
    printWriter.println("}");
    printWriter.println();
  }

  private boolean isClass() {
    return name != null;
  }
}
