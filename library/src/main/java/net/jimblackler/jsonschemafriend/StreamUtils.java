package net.jimblackler.jsonschemafriend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamUtils {
  /**
   * Loads all the text from an input string and returns the result as a string.
   *
   * @param inputStream The stream to read.
   * @return All of the text from the stream.
   * @throws IOException Thrown if a read error occurs.
   */
  public static String streamToString(InputStream inputStream) throws IOException {
    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
         BufferedReader reader = new BufferedReader(inputStreamReader)) {
      String newline = System.lineSeparator();
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (output.length() > 0) {
          output.append(newline);
        }
        output.append(line);
      }
      return output.toString();
    }
  }
}
