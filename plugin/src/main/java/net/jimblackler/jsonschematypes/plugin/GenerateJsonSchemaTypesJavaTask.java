package net.jimblackler.jsonschematypes.plugin;

import java.nio.file.Path;
import net.jimblackler.jsonschematypes.DocumentSource;
import net.jimblackler.jsonschematypes.GenerationException;
import net.jimblackler.jsonschematypes.Main2;
import net.jimblackler.jsonschematypes.SchemaStore;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() {
    Path outPath = Common.getCodePath(getProject());

    Project project = getProject();
    SourceSet mainSourceSet = ((SourceSetContainer) project.getProperties().get("sourceSets"))
                                  .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

    Path resources = mainSourceSet.getOutput().getResourcesDir().toPath();

    JsonSchemaTypesPluginExtension extension =
        project.getExtensions().getByType(JsonSchemaTypesPluginExtension.class);

    resources = resources.resolve(extension.getResourcesPath());

    try {
      SchemaStore schemaStore = new SchemaStore(new DocumentSource());
      schemaStore.loadResources(resources);
      Main2.outputTypes(outPath, schemaStore, extension.getPackageOut());
    } catch (GenerationException e) {
      throw new GradleException("Error generating types", e);
    }
  }
}
