package net.jimblackler.jsonschemafriend;

import org.json.JSONObject;

public class StandardGenerationException extends GenerationException {
  private final JSONObject standardOutput;

  public StandardGenerationException(JSONObject standardOutput) {
    super(standardOutput.toString());
    this.standardOutput = standardOutput;
  }

  public JSONObject getStandardOutput() {
    return standardOutput;
  }
}
