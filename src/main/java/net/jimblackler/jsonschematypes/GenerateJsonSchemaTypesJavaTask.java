package net.jimblackler.jsonschematypes;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() throws IOException {
    System.out.println("In the task action");
    JsonSchemaTypesPluginExtension extension =
        getProject().getExtensions().getByType(JsonSchemaTypesPluginExtension.class);
    System.out.println("Hello, " + extension.getGreeter());
    System.out.println("I have a message for You: " + extension.getMessage());

    Path outPath =
        getProject().getBuildDir().toPath().resolve("generated").resolve("sources").resolve("jst");

    Files.createDirectory(outPath);
    Path exampleFile = outPath.resolve("test.txt");
    List<String> lines = Arrays.asList("The first line", "The second line");
    Files.write(exampleFile, lines, StandardCharsets.UTF_8);
  }
}
