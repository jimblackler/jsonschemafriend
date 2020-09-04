package net.jimblackler.jsonschemafriend;

import javax.script.ScriptException;

public class Ecma262Pattern {
  private static final javax.script.ScriptEngine SCRIPT_ENGINE =
      new javax.script.ScriptEngineManager().getEngineByName("js");
  private final String pattern;

  public Ecma262Pattern(String pattern) {
    this.pattern = pattern;
  }

  public boolean matches(String text) {
    SCRIPT_ENGINE.put("pattern", pattern);
    SCRIPT_ENGINE.put("text", text);

    try {
      return SCRIPT_ENGINE.eval("text.match(pattern)") != null;
    } catch (ScriptException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public String toString() {
    return pattern;
  }
}
