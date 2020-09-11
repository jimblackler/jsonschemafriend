package net.jimblackler.jsonschematypes.codegen;

import net.jimblackler.jsonschemafriend.Schema;

public interface CodeGenerator {
  void build(Schema schema) throws CodeGenerationException;
}
