package net.jimblackler.jsonschematypes.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;

public class JsonSchemaTypesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {

    project.getExtensions().create("jsonSchemaTypes", JsonSchemaTypesPluginExtension.class);
    project.afterEvaluate(
        project1 -> {
          GenerateJsonSchemaTypesJavaTask task =
              project1.getTasks().create("generateJsonSchemaTypes",
              GenerateJsonSchemaTypesJavaTask.class);
          task.setGroup("build");
          task.dependsOn(project1.getTasks().getByName("processResources"));  // needed?
          project1.getTasks().getByName("compileJava").dependsOn(task);  // needed?

          Path outPath = Common.getCodePath(project1);
          SourceSet mainSourceSet =
              ((SourceSetContainer) project1.getProperties().get("sourceSets")).
                  getByName(SourceSet.MAIN_SOURCE_SET_NAME);
          SourceDirectorySet java = mainSourceSet.getJava();
          Collection<File> srcDirs = new HashSet<>(java.getSrcDirs());
          srcDirs.add(outPath.toFile());
          java.setSrcDirs(srcDirs);
        });
  }
}