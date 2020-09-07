package net.jimblackler.jsonschemafriend;

import java.net.URI;

public class ConstMismatchError extends ValidationError {
  private final Object _const;

  public ConstMismatchError(URI uri, Object document, Object _const, Schema schema) {
    super(uri, document, "Expected const: " + _const, schema);
    this._const = _const;
  }

  public Object getConst() {
    return _const;
  }
}
