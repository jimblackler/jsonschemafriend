package net.jimblackler.jsonschemafriend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class PathUtilsTest {
  private static final int MAX_LENGTH = 100;

  @TestFactory
  Collection<DynamicTest> pathTest() {
    Collection<DynamicTest> allFileTests = new ArrayList<>();
    for (int idx = 0; idx != 100; idx++) {
      int seed = idx;
      allFileTests.add(dynamicTest("Seed " + seed, () -> {
        Random random = new Random(seed);
        test(random);
      }));
    }

    return allFileTests;
  }

  private void test(Random random) throws MissingPathException {
    Map<String, Object> jsonObject = new LinkedHashMap<>();
    String str = randomString(random, random.nextInt(MAX_LENGTH) + 1);
    String testInsertion = "hello";
    jsonObject.put(str, testInsertion);

    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject));

    URI uri = URI.create("");
    URI appended = PathUtils.append(uri, str);

    Object o = PathUtils.fetchFromPath(jsonObject, appended.getRawFragment());
    assertTrue(o instanceof String);
    assertEquals(o, testInsertion);
  }

  private String randomString(Random random, int length) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) random.nextInt());
    }
    // We don't aim to handle strings that won't survive URL encoding with standard methods.
    String urlEncoded = URLEncoder.encode(stringBuilder.toString());
    return URLDecoder.decode(urlEncoded);
  }
}
