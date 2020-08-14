package net.jimblackler.jsonschematypes;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void action() {
    System.out.println("In the task action");
  }
}
