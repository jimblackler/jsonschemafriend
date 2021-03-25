package net.jimblackler.jsonschemafriend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

public class StandardValidationException extends ValidationException {
  private final Map<String, Object> standardOutput;

  public StandardValidationException(Map<String, Object> standardOutput) {
    this.standardOutput = standardOutput;
  }

  @Override
  public String toString() {
    try {
      return new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .writeValueAsString(standardOutput);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Map<String, Object> getStandardOutput() {
    return standardOutput;
  }
}
