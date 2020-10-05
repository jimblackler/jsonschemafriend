package net.jimblackler.jsonschemafriend;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;

public class Ecma262Pattern implements RegExPattern {
  private static final Context GRAALVM_CONTEXT = Context.create("js");
  private final String pattern;
  private final Value function;

  public Ecma262Pattern(String pattern) throws InvalidRegexException {
    this.pattern = pattern;
    function = test(pattern);
  }

  private Value test(String pattern) throws InvalidRegexException {
    final Value function;
    try {
      synchronized (GRAALVM_CONTEXT) {
        function = GRAALVM_CONTEXT
                       .eval("js",
                           "pattern => {const regex = new RegExp(pattern, 'u');"
                               + "return text => text.match(regex)}")
                       .execute(pattern);
      }
    } catch (PolyglotException ex) {
      throw new InvalidRegexException(ex);
    }
    return function;
  }

  @Override
  public boolean matches(String text) {
    synchronized (GRAALVM_CONTEXT) {
      return !function.execute(text).isNull();
    }
  }

  @Override
  public String toString() {
    return pattern;
  }
}
