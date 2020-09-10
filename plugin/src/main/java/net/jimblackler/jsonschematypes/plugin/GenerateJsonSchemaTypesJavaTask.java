package net.jimblackler.jsonschematypes.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.jimblackler.jsonschematypes.codegen.CodeGeneration;
import net.jimblackler.jsonschematypes.codegen.CodeGenerationException;
import net.jimblackler.jsonschematypes.codegen.CodeGenerator;
import net.jimblackler.jsonschematypes.codegen.FileUtils;
import net.jimblackler.jsonschematypes.codegen.JavaCodeGenerator;
import net.jimblackler.jsonschematypes.codegen.MultiGenerator;
import net.jimblackler.jsonschematypes.codegen.TypeScriptCodeGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

public class GenerateJsonSchemaTypesJavaTask extends DefaultTask {
  @TaskAction
  public void generate() throws IOException, CodeGenerationException {
    Project project = getProject();
    SourceSet mainSourceSet = ((SourceSetContainer) project.getProperties().get("sourceSets"))
                                  .getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    JsonSchemaTypesPluginExtension extension =
        project.getExtensions().getByType(JsonSchemaTypesPluginExtension.class);
    File resourcesDir = mainSourceSet.getOutput().getResourcesDir();
    Path resources = resourcesDir.toPath().resolve(extension.getResourcesPath());
    Path codePath = Common.getCodePath(getProject());

    FileUtils.createOrEmpty(codePath);
    JavaCodeGenerator javaCodeGenerator = new JavaCodeGenerator(extension.getPackageOut());
    TypeScriptCodeGenerator typeScriptCodeGenerator = new TypeScriptCodeGenerator();
    List<CodeGenerator> generators = new ArrayList<>();
    generators.add(javaCodeGenerator);
    generators.add(typeScriptCodeGenerator);
    CodeGeneration.build(resources.toUri().toURL(), new MultiGenerator(generators));
    javaCodeGenerator.output(codePath);
    typeScriptCodeGenerator.output(codePath);
  }
}
