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
        <version>0.7.2</version>
    </dependency>
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20200518</version>
    </dependency>
```

# Usage

## Via a JSONObject.

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
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      Schema schema = schemaStore.loadSchema(schemaJson); // Load the schema.
      Validator.validate(schema, 1); // Will not throw an exception.
      Validator.validate(schema, "X"); // Will throw a ValidationException.
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```

## Via Java Resources.

This example loads a schema in the `resources` folder and validates data in the
`resources` folder.

### `schema.json`

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "properties": {
    "name": {
      "type": "string",
      "minLength": 2
    }
  }
}
```

### `data1.json`

```json
{
  "name": "Bill"
}
```

### `data2.json`

```json
{
  "name": ""
}
```

### `Main.java`

```java
import java.io.IOException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      // Load the schema.
      Schema schema = schemaStore.loadSchema(Main.class.getResource("/schema.json"));

      // Will not throw an exception.
      Validator.validate(schema, Main.class.getResourceAsStream("/data1.json"));

      // Will throw a ValidationException.
      Validator.validate(schema, Main.class.getResourceAsStream("/data2.json"));
    } catch (SchemaException | IOException e) {
      e.printStackTrace();
    }
  }
}
```

## From URIs or URLs.

This example loads both the schema and the data to test from the internet, via
URIs (URLs can also be use).

```java
import java.io.IOException;
import java.net.URI;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      // Load the schema.
      Schema schema = schemaStore.loadSchema(URI.create("https://json.schemastore.org/resume"));

      // Will not throw an exception; document passes the schema.
      URI resume = URI.create(
          "https://gist.githubusercontent.com/thomasdavis/c9dcfa1b37dec07fb2ee7f36d7278105/raw");
      Validator.validate(schema, resume);

    } catch (SchemaException | IOException e) {
      e.printStackTrace();
    }
  }
}
```