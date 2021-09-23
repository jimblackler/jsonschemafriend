package net.jimblackler.jsonschemafriend;

import static org.joni.Matcher.FAILED;

import java.nio.charset.StandardCharsets;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Option;
import org.joni.Regex;
import org.joni.exception.ValueException;

public class JoniRegExPattern implements RegExPattern {
  private final Regex regex;
  private final String pattern;

  public JoniRegExPattern(String pattern) throws InvalidRegexException {
    this.pattern = pattern;
    byte[] bytes = pattern.getBytes(StandardCharsets.UTF_8);
    try {
      regex = new Regex(bytes, 0, bytes.length, Option.NONE, UTF8Encoding.INSTANCE);
    } catch (ValueException e) {
      throw new InvalidRegexException(e);
    }
  }

  @Override
  public boolean matches(String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    return regex.matcher(bytes).search(0, bytes.length, Option.DEFAULT) != FAILED;
  }

  @Override
  public String toString() {
    return pattern;
  }
}
