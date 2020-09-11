package net.jimblackler.jsonschematypes.codegen;

import net.jimblackler.jsonschemafriend.Schema;

public class MultiGenerator implements CodeGenerator {
  private final Iterable<CodeGenerator> generators;

  public MultiGenerator(Iterable<CodeGenerator> generators) {
    this.generators = generators;
  }

  @Override
  public void build(Schema schema) throws CodeGenerationException {
    for (CodeGenerator generator : generators) {
      generator.build(schema);
    }
  }
}
