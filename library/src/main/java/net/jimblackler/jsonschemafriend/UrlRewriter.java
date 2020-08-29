package net.jimblackler.jsonschemafriend;

import java.net.URI;

public interface UrlRewriter {
  URI rewrite(URI in);
}