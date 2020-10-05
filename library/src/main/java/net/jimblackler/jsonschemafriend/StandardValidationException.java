package net.jimblackler.jsonschemafriend;

import org.json.JSONObject;

public class StandardValidationException extends ValidationException {
  private final JSONObject standardOutput;

  public StandardValidationException(JSONObject standardOutput) {
    super(standardOutput.toString(2));
    this.standardOutput = standardOutput;
  }

  public JSONObject getStandardOutput() {
    return standardOutput;
  }
}
