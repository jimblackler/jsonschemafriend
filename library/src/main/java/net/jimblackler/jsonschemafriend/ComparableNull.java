package net.jimblackler.jsonschemafriend;

public class ComparableNull {
  @Override
  public boolean equals(Object obj) {
    return obj instanceof ComparableNull;
  }
}
