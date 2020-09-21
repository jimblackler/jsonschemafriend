package net.jimblackler.jsonschemafriend;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class Ecma262Pattern {
  private static final Context GRAALVM_CONTEXT = Context.create();
  private final String pattern;
  private final Value function;

  public Ecma262Pattern(String pattern) throws InvalidRegexException {
    this.pattern = pattern;
    try {
      function = GRAALVM_CONTEXT
                     .eval("js",
                         "pattern => {const regex = new RegExp(pattern, 'u');"
                             + "return text => text.match(regex)}")
                     .execute(pattern);
    } catch (PolyglotException ex) {
      throw new InvalidRegexException(ex);
    }
  }

  public boolean matches(String text) {
    return !function.execute(text).isNull();
  }

  @Override
  public String toString() {
    return pattern;
  }
}
