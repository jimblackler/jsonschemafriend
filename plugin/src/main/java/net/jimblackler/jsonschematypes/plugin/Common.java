package net.jimblackler.jsonschematypes.plugin;

import org.gradle.api.Project;

import java.nio.file.Path;

public class Common {
  public static Path getCodePath(Project project) {
    return project.getBuildDir().toPath().resolve("generated").resolve("sources").resolve("jst");
  }
}
