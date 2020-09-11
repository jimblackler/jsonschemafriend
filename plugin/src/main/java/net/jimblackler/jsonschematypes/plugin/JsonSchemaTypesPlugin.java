package net.jimblackler.jsonschematypes.plugin;

import static net.jimblackler.jsonschematypes.plugin.Common.getCodePath;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class JsonSchemaTypesPlugin implements Plugin<Project> {
  @Override
  public void apply(Project prejectIn) {
    prejectIn.getExtensions().create("jsonSchemaTypes", JsonSchemaTypesPluginExtension.class);
    prejectIn.afterEvaluate(project -> {
      GenerateJsonSchemaTypesJavaTask task = project.getTasks().create(
          "generateJsonSchemaTypes", GenerateJsonSchemaTypesJavaTask.class);
      task.setGroup("build");
      task.dependsOn(project.getTasks().getByName("processResources"));
      project.getTasks().getByName("compileJava").dependsOn(task);

      Path outPath = getCodePath(project);

      SourceSet mainSourceSet = ((SourceSetContainer) project.getProperties().get("sourceSets"))
                                    .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
      {
        SourceDirectorySet java = mainSourceSet.getJava();
        Collection<File> srcDirs = new HashSet<>(java.getSrcDirs());
        srcDirs.add(outPath.resolve("java").toFile());
        java.setSrcDirs(srcDirs);
      }
      {
        SourceDirectorySet resources = mainSourceSet.getResources();
        Collection<File> srcDirs = new HashSet<>(resources.getSrcDirs());
        srcDirs.add(outPath.resolve("typescript").toFile());
        resources.setSrcDirs(srcDirs);
      }
    });
  }
}