package net.jimblackler.jsonschematypes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.json.JSONArray;
import org.json.JSONObject;

public class SuiteTest {
  public static void main(String[] args) {
    Path base = Path.of("/suites");
    scan(base.resolve("draft7"));
  }

  private static void scan(Path testDir) {
    System.out.println();
    System.out.println(testDir);
    System.out.println();

    try (InputStream inputStream = ExampleTest.class.getResourceAsStream(testDir.toString());
         BufferedReader bufferedReader =
             new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String resource;

      while ((resource = bufferedReader.readLine()) != null) {
        if (resource.endsWith(".json")) {
          JSONArray data = (JSONArray) Utils.getJsonObject(testDir.resolve(resource).toString());
          for (int idx = 0; idx != data.length(); idx++) {
            JSONObject jsonObject = data.getJSONObject(idx);
            if (jsonObject.has("schema")) {
            }
          }
        }
      }
    } catch (IOException | GenerationException e) {
      throw new IllegalStateException(e);
    }
    JSONObject outSchema = new JSONObject();
    System.out.println(outSchema.toString(2));
  }
}
