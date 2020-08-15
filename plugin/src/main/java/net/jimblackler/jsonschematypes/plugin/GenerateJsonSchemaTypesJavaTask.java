package net.jimblackler.jsonschematypes.plugin;

import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Main;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() throws IOException {
    System.out.println("In the task action");
    JsonSchemaTypesPluginExtension extension =
        getProject().getExtensions().getByType(JsonSchemaTypesPluginExtension.class);
    System.out.println("Hello, " + extension.getGreeter());
    System.out.println("I have a message for You: " + extension.getMessage());

    Path outPath = Common.getCodePath(getProject());

    if (!Files.exists(outPath)) {
      Files.createDirectories(outPath);
    }

    try {
      Main.writeFile(outPath);
    } catch (GenerationException e) {
      throw new GradleException("Error generating types", e);
    }
  }
}
