package net.jimblackler.jsonschematypes.codegen;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.jimblackler.jsonschemafriend.Schema;

public class TypeScriptCodeGenerator implements CodeGenerator {
  private final Map<URI, TypeScriptBuilder> builtClasses = new HashMap<>();

  TypeScriptBuilder get(Schema schema) {
    URI uri = schema.getUri();
    if (builtClasses.containsKey(uri)) {
      return builtClasses.get(uri);
    }

    return new TypeScriptBuilder(this, schema);
  }

  public void register(URI uri, TypeScriptBuilder typeScriptBuilder) {
    builtClasses.put(uri, typeScriptBuilder);
  }

  @Override
  public void build(Schema schema) {
    get(schema);
  }

  public void output(OutputStream stream) {
    try (PrintWriter printWriter = new PrintWriter(stream)) {
      _output(printWriter);
    }
  }

  public void output(Path out) throws IOException {
    out.toFile().mkdirs();
    try (PrintWriter printWriter = new PrintWriter(out.resolve("types.ts").toFile())) {
      _output(printWriter);
    }
  }

  private void _output(PrintWriter printWriter) {
    for (Map.Entry<URI, TypeScriptBuilder> entry : new HashSet<>(builtClasses.entrySet())) {
      TypeScriptBuilder builder = entry.getValue();
      if (builder.getParent() == null) {
        builder.write(printWriter, 0);
      }
    }
  }
}
