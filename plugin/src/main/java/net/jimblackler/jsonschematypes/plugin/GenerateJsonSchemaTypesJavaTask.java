package net.jimblackler.jsonschematypes.plugin;

import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Main;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Path;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() throws IOException {
    Path outPath = Common.getCodePath(getProject());

    Project project = getProject();
    SourceSet mainSourceSet =
        ((SourceSetContainer) project.getProperties().get("sourceSets")).
            getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    Path resources = mainSourceSet.getOutput().getResourcesDir().toPath();

    JsonSchemaTypesPluginExtension extension =
        project.getExtensions().getByType(JsonSchemaTypesPluginExtension.class);

    resources = resources.resolve(extension.getResourcesPath());

    try {
      Main.writeFile(outPath, resources, extension.getPackageOut());
    } catch (GenerationException e) {
      throw new GradleException("Error generating types", e);
    }
  }
}
