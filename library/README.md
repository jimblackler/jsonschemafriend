# jsonschemafriend

jsonschemafriend is a JSON Schema loader and validator, delivered as a Java
library.

It is written by <jimblackler@gmail.com> and offered under an
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

It is compatible with the following metaschemas

*   http://json-schema.org/draft-03/schema#
*   http://json-schema.org/draft-04/schema#
*   http://json-schema.org/draft-06/schema#
*   http://json-schema.org/draft-07/schema#

# Including in a project

## Gradle

To include in a Gradle project, ensure jitpack repository is specified in your
base `build.gradle` file. For example:

```groovy
repositories {
    maven {
        url 'https://jitpack.io'
    }
    // ...
}
```

Add the project as a dependency in the module's `build.gradle`. Usage also
requires the `org.json` library.

```groovy
dependencies {
    implementation 'com.github.jimblackler.jsonschematypes:jsonschemafriend:0.7'
    implementation 'org.json:json:20200518'
    // ...
}
```

## Maven

```xml
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
```

```xml
    <dependency>
        <groupId>com.github.jimblackler.jsonschematypes</groupId>
        <artifactId>codegen</artifactId>
        <version>0.7.1</version>
    </dependency>
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20200518</version>
    </dependency>
```

# Usage

This is an example of loading a schema in a JSONObject.

```java
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONObject;

public class Main {
  public static void main(String[] args) {
    // Create a new schema in a JSON object.
    JSONObject schemaJson = new JSONObject();
    schemaJson.put("$schema", "http://json-schema.org/draft-07/schema#");
    schemaJson.put("type", "integer");

    try {
      SchemaStore schemaStore = new SchemaStore();  // Initialize a SchemaStore.
      Schema schema = schemaStore.loadSchema(schemaJson); // Load the schema.
      Validator.validate(schema, 1);  // Will not throw an exception.
      Validator.validate(schema, "x");  // Will throw a ValidationException.
    } catch (SchemaException e) {
      // ...
    }
  }
}
```
