package net.jimblackler.jsonschemafriend;

import java.util.WeakHashMap;

public class CachedRegExPatternSupplier implements RegExPatternSupplier {
  private final RegExPatternSupplier wrapped;
  private final WeakHashMap<String, RegExPattern> map = new WeakHashMap<>();

  CachedRegExPatternSupplier(RegExPatternSupplier wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public RegExPattern newPattern(String pattern) throws InvalidRegexException {
    RegExPattern regExPattern1 = map.get(pattern);
    if (regExPattern1 == null) {
      regExPattern1 = wrapped.newPattern(pattern);
      map.put(pattern, regExPattern1);
    }
    return regExPattern1;
  }
}
