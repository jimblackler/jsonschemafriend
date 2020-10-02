package net.jimblackler.jsonschematypes.codegen;

import com.helger.jcodemodel.JCodeModelException;
import com.helger.jcodemodel.JDefinedClass;

class JavaDefinedClassMaker {
  static JDefinedClass makeClassForSchema(String name, Client client) {
    JDefinedClass _class;
    while (true) {
      try {
        _class = client.getClass(name);
        break;
      } catch (JCodeModelException e) {
        name = varyName(name);
      }
    }
    return _class;
  }

  private static String varyName(String name) {
    for (int idx = 0; idx < name.length(); idx++) {
      try {
        int i = Integer.parseInt(name.substring(idx));
        return name.substring(0, idx) + (i + 1);
      } catch (NumberFormatException e) {
        // Ignored by design.
      }
    }
    return name + "2";
  }

  interface Client {
    JDefinedClass getClass(String name) throws JCodeModelException;
  }
}
