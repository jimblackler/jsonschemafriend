package net.jimblackler.jsonschemafriend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

public class PathUtilsTest {
  @TestFactory
  Collection<DynamicTest> pathTestBasic() {
    return doTest(200, 50, 32, 128);
  }

  @TestFactory
  Collection<DynamicTest> pathTest() {
    return doTest(200, 100, 0, Integer.MAX_VALUE);
  }

  @Test
  void test() throws Exception {
    test0("^[a-zA-Z0-9_-]+$");
  }

  private Collection<DynamicTest> doTest(int numberTests, int length, int minRange, int maxRange) {
    Collection<DynamicTest> allFileTests = new ArrayList<>();
    for (int idx = 0; idx != numberTests; idx++) {
      int seed = idx;
      allFileTests.add(dynamicTest("Seed " + seed, () -> {
        Random random = new Random(seed);
        test0(randomString(random, length, minRange, maxRange));
      }));
    }

    return allFileTests;
  }

  private void test0(String str) throws JsonProcessingException, MissingPathException {
    String testInsertion = "hello";
    String level0Name = "a";
    String level2Name = "b";

    Map<String, Object> level0 = new LinkedHashMap<>();
    Map<String, Object> level1 = new LinkedHashMap<>();
    Map<String, Object> level2 = new LinkedHashMap<>();

    level0.put(level0Name, level1);
    level1.put(str, level2);
    level2.put(level2Name, testInsertion);

    System.out.println(
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(level0));

    URI uri = URI.create("");
    uri = PathUtils.append(uri, level0Name);
    uri = PathUtils.append(uri, str);
    uri = PathUtils.append(uri, level2Name);

    System.out.println(uri);

    Object o = PathUtils.fetchFromPath(level0, uri.getRawFragment());
    assertTrue(o instanceof String);
    assertEquals(o, testInsertion);
  }

  private static String randomString(Random random, int length, int minRange, int maxRange) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int idx = 0; idx != length; idx++) {
      stringBuilder.append((char) (random.nextInt(maxRange - minRange) + minRange));
    }
    // We don't aim to handle strings that won't survive URL encoding with standard methods.
    String urlEncoded = URLEncoder.encode(stringBuilder.toString());
    return URLDecoder.decode(urlEncoded);
  }
}
