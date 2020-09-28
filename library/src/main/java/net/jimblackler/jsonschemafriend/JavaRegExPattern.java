package net.jimblackler.jsonschemafriend;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class JavaRegExPattern implements RegExPattern {
  private final Pattern pattern;

  public JavaRegExPattern(String pattern) throws InvalidRegexException {
    try {
      this.pattern = Pattern.compile(pattern);
    } catch (PatternSyntaxException ex) {
      throw new InvalidRegexException(ex);
    }
  }

  @Override
  public boolean matches(String text) {
    return pattern.matcher(text).matches();
  }

  @Override
  public String toString() {
    return pattern.toString();
  }
}
