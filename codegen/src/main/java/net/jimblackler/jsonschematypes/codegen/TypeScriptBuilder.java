package net.jimblackler.jsonschematypes.codegen;

import static net.jimblackler.jsonschematypes.codegen.NameUtils.nameForSchema;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import net.jimblackler.jsonschemafriend.CombinedSchema;
import net.jimblackler.jsonschemafriend.Schema;

public class TypeScriptBuilder {
  private final String baseClassName;
  private final String fullClassName;
  private final Schema schema;
  private final TypeScriptBuilder parent;
  private final Collection<TypeScriptBuilder> children = new ArrayList<>();
  private final String typeName;
  private final TypeScriptCodeGenerator typeScriptCodeGenerator;

  public TypeScriptBuilder(TypeScriptCodeGenerator typeScriptCodeGenerator, Schema schema) {
    typeScriptCodeGenerator.register(schema.getUri(), this);

    this.schema = schema;
    this.typeScriptCodeGenerator = typeScriptCodeGenerator;
    Schema parent = schema.getParent();
    if (parent == null) {
      this.parent = null;
    } else {
      this.parent = typeScriptCodeGenerator.get(parent);
      this.parent.addChild(this);
    }

    CombinedSchema combinedSchema = new CombinedSchema(schema);
    Collection<String> types = combinedSchema.getInferredTypes();
    baseClassName = nameForSchema(schema);
    if (this.parent == null) {
      fullClassName = baseClassName;
    } else {
      fullClassName = this.parent.getFullClassName() + "." + baseClassName;
    }

    Schema items = schema.getItems();
    if (items != null) {
      typeScriptCodeGenerator.build(items);
    }

    {
      StringBuilder sb = new StringBuilder();
      Collection<String> types0 = new HashSet<>(types);
      if (types0.contains("integer")) {
        types0.remove("integer");
        types0.add("number");
      }
      for (String type : types0) {
        if (isClass()) {
          if (sb.length() > 0) {
            sb.append(" | ");
          }
          sb.append(fullClassName);
        } else if ("array".equals(type)) {
          if (sb.length() > 0) {
            sb.append(" | ");
          }
          if (items == null) {
            sb.append("Object");
          } else {
            TypeScriptBuilder typeScriptBuilder = typeScriptCodeGenerator.get(items);
            sb.append(typeScriptBuilder.getTypeName());
          }
          sb.append("[]");
        } else {
          if (sb.length() > 0) {
            sb.append(" | ");
          }
          sb.append(type);
        }
      }
      typeName = sb.toString();
    }

    for (Map.Entry<String, Schema> entry : combinedSchema.getProperties().entrySet()) {
      typeScriptCodeGenerator.build(entry.getValue());
    }
  }

  static void writeLine(PrintWriter printWriter, int indentationLevel, String line) {
    for (int idx = 0; idx != indentationLevel; idx++) {
      printWriter.print("  ");
    }
    printWriter.println(line);
  }

  private String getFullClassName() {
    return fullClassName;
  }

  private String getTypeName() {
    return typeName;
  }

  private void addChild(TypeScriptBuilder child) {
    children.add(child);
  }

  private boolean isClass() {
    return !schema.getProperties().isEmpty();
  }

  void write(PrintWriter printWriter, int indentationLevel) {
    if (isClass()) {
      writeLine(printWriter, indentationLevel, "// Generated from " + schema.getUri());
      writeLine(printWriter, indentationLevel, "export class " + baseClassName + " {");

      for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
        TypeScriptBuilder builder = typeScriptCodeGenerator.get(entry.getValue());
        String field = entry.getKey();
        if (field.contains("-")) {
          field = "\"" + field + "\"";
        }
        writeLine(printWriter, indentationLevel + 1, field + ": " + builder.getTypeName() + ";");
      }
      writeLine(printWriter, indentationLevel, "}");
    }

    boolean childClasses = false;
    for (TypeScriptBuilder child : children) {
      if (child.isClass()) {
        childClasses = true;
        break;
      }
    }

    if (childClasses) {
      writeLine(printWriter, indentationLevel, "export namespace " + baseClassName + " {");
      for (TypeScriptBuilder child : children) {
        child.write(printWriter, indentationLevel + 1);
      }
      writeLine(printWriter, indentationLevel, "}");
    }
  }

  public TypeScriptBuilder getParent() {
    return parent;
  }

  @Override
  public String toString() {
    return schema.toString();
  }
}
