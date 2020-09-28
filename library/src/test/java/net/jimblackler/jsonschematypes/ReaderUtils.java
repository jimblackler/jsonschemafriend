package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Consumer;

public class ReaderUtils {
  static void getLines(Reader reader, Consumer<String> consumer) {
    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        consumer.accept(line);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  static void getLines(InputStream stream, Consumer<String> consumer) {
    getLines(new BufferedReader(new InputStreamReader(stream)), consumer);
  }
}
