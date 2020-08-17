package net.jimblackler.jsonschematypes.plugin;

import java.nio.file.Path;
import org.gradle.api.Project;

public class Common {
  public static Path getCodePath(Project project) {
    return project.getBuildDir().toPath().resolve("generated").resolve("sources").resolve("jst");
  }
}
