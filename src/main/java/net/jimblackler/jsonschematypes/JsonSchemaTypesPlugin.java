package net.jimblackler.jsonschematypes;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class JsonSchemaTypesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    JsonSchemaTypesPluginExtension extension =
        project.getExtensions().create("jsonSchemaTypes", JsonSchemaTypesPluginExtension.class);
    project.afterEvaluate(
        project1 -> project1.getTasks().create("generateJsonSchemaTypes",
            GenerateJsonSchemaTypesJavaTask.class));

    project.task("hello").doLast(task -> {
          System.out.println("Hello, " + extension.getGreeter());
          System.out.println("I have a message for You: " + extension.getMessage());
        }
    );
  }
}