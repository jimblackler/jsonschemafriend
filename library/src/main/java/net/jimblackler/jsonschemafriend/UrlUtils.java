package net.jimblackler.jsonschemafriend;

import com.sun.org.apache.xalan.internal.res.XSLTErrorResources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

public class UrlUtils {
  static String readFromStream(URL url) throws IOException {
    String result;
    try (InputStream stream = url.openStream()) {
      result = streamToString(stream);
    } catch (final IOException e) {
      System.err.println(e.getMessage());
      e.printStackTrace(System.err);
      throw e;
    }
    if (result.isEmpty() && "http".equals(url.getProtocol())) {
      // in case tried http and received empty content, try to connect to same url with https
      URL secureUrl = new URL(url.toString().replaceFirst("http", "https"));
      try (InputStream stream = secureUrl.openStream()) {
        result = streamToString(stream);
      }
    }
    return result;
  }
}
