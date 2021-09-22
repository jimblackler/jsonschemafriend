package net.jimblackler.jsonschemafriend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

  static File streamToTempFile(InputStream stream) throws IOException {
    File tempFile = File.createTempFile("jsf", ".txt");
    tempFile.deleteOnExit();
    try (OutputStream outStream = new FileOutputStream(tempFile);) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = stream.read(buffer)) != -1) {
        outStream.write(buffer, 0, bytesRead);
      }
    }
    return tempFile;
  }
}
