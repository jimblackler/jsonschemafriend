package net.jimblackler.jsonschematypes;

import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Main {
  public static void writeFile(Path outPath, Path resources, String _package)
      throws GenerationException {
    try {

      if (Files.exists(outPath)) {
        // Empty the directory if it already exists.
        try (Stream<Path> files = Files.walk(outPath)) {
          files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
      }
      Files.createDirectories(outPath);

      JCodeModel codeModel = new JCodeModel();
      JPackage jp = codeModel._package(_package);

      try (Stream<Path> files = Files.walk(resources)) {
        files.forEach(path -> {
          if (!Files.isDirectory(path)) {
            try {

              String s = path.getFileName().toString().split("\\.")[0];
              String converted = s.substring(0, 1).toUpperCase() + s.substring(1);

              // Create a new class
              JDefinedClass jc = jp._class(converted);

              // Implement Serializable
              jc._implements(Serializable.class);

              // Add Javadoc
              jc.javadoc().add("A JCodeModel example.");

              // Add default constructor
              jc.constructor(JMod.PUBLIC).javadoc().add("Creates a new " + jc.name() + ".");

              // Add constant serializable id
              jc.field(JMod.STATIC | JMod.FINAL, Long.class, "serialVersionUID", JExpr.lit(1L));

              // Add private variable
              JFieldVar quantity = jc.field(JMod.PRIVATE, Integer.class, "quantity");

              // Add get method
              JMethod getter = jc.method(JMod.PUBLIC, quantity.type(), "getQuantity");
              getter.body()._return(quantity);
              getter.javadoc().add("Returns the quantity.");
              getter.javadoc().addReturn().add(quantity.name());

              // Add set method
              JMethod setter = jc.method(JMod.PUBLIC, codeModel.VOID, "setQuantity");
              setter.param(quantity.type(), quantity.name());
              setter.body().assign(JExpr._this().ref(quantity.name()), JExpr.ref(quantity.name()));
              setter.javadoc().add("Set the quantity.");
              setter.javadoc().addParam(quantity.name()).add("the new quantity");

              // Generate the code
              codeModel.build(outPath.toFile());
            } catch (IOException | JClassAlreadyExistsException e) {
              throw new GenerationUncheckedException(e);
            }
          }
        });
      }
    } catch (GenerationUncheckedException | IOException ex) {
      throw new GenerationException(ex);
    }
  }
}
