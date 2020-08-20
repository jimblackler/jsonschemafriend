package net.jimblackler.jsonschematypes;

import java.net.URI;

interface UrlRewriter {
  URI rewrite(URI in);
}