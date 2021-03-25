package net.jimblackler.jsonschemafriend;

import java.util.Map;

public class StandardGenerationException extends GenerationException {
  private final Map<String, Object> standardOutput;

  public StandardGenerationException(Map<String, Object> standardOutput) {
    super(standardOutput.toString());
    this.standardOutput = standardOutput;
  }

  public Map<String, Object> getStandardOutput() {
    return standardOutput;
  }
}
