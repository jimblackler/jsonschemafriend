package net.jimblackler.jsonschematypes;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;

public class JsonSchemaTypesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {

    project.getExtensions().create("jsonSchemaTypes", JsonSchemaTypesPluginExtension.class);
    project.afterEvaluate(
        project1 -> {
          GenerateJsonSchemaTypesJavaTask task = project1.getTasks().create("generateJsonSchemaTypes",
              GenerateJsonSchemaTypesJavaTask.class);
          task.setGroup("build");
          task.dependsOn(project1.getTasks().getByName("processResources"));  // needed?
          project1.getTasks().getByName("compileJava").dependsOn(task);  // needed?

          if (false) {
            SourceSetContainer sourceSets =
                (SourceSetContainer) project1.getProperties().get("sourceSets");
            SourceSet main = sourceSets.getByName("main");
            SourceDirectorySet java = main.getJava();
            File buildDir1 = project1.getBuildDir();
            java.srcDir(buildDir1.getAbsolutePath());
          }
        });
  }
}