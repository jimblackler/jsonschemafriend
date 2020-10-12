package net.jimblackler.jsonschemafriend;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class Ecma262Pattern implements RegExPattern {
  private static final Value REGEX_BUILDER = Context.create("js").eval("js",
      "pattern => {"
          + "  let regex;"
          + "  try {"
          + "    regex = new RegExp(pattern, 'u');"
          + "  } catch (e) {"
          + "    regex = new RegExp(pattern);"
          + "  }"
          + "  return text => text.match(regex)"
          + "};");
  private final String pattern;
  private final Value function;

  public Ecma262Pattern(String pattern) throws InvalidRegexException {
    this.pattern = pattern;

    try {
      synchronized (REGEX_BUILDER) {
        function = REGEX_BUILDER.execute(pattern);
      }
    } catch (PolyglotException ex) {
      throw new InvalidRegexException(ex);
    }
  }

  @Override
  public boolean matches(String text) {
    synchronized (REGEX_BUILDER) {
      return !function.execute(text).isNull();
    }
  }

  @Override
  public String toString() {
    return pattern;
  }
}
