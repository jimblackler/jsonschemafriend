package net.jimblackler.jsonschemafriend;

import static net.jimblackler.jsonschemafriend.StreamUtils.streamToString;

import java.io.IOException;
import java.io.InputStream;
import net.jimblackler.usejson.Json5Parser;

public class DocumentUtils {
  public static <T> T loadJson(InputStream inputStream) throws IOException {
    return parseJson(streamToString(inputStream));
  }

  public static <T> T parseJson(String content) {
    return new Json5Parser().parse(content);
  }
}
