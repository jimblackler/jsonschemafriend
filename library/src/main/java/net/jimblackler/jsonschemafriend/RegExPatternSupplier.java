package net.jimblackler.jsonschemafriend;

public interface RegExPatternSupplier {
  RegExPattern newPattern(String pattern) throws InvalidRegexException;
}
