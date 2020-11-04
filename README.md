# jsonschemafriend

jsonschemafriend is a JSON Schema loader and validator, delivered as a Java
library.

An online demonstration [is here](https://tryjsonschematypes.appspot.com/#validate).

It is written by <jimblackler@gmail.com> and offered under an
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

It is compatible with the following metaschemas

*   http://json-schema.org/draft-03/schema#
*   http://json-schema.org/draft-04/schema#
*   http://json-schema.org/draft-06/schema#
*   http://json-schema.org/draft-07/schema#
*   https://json-schema.org/draft/2019-09/schema

# Including in a project

The library is live on JitPack.

[![](https://jitpack.io/v/net.jimblackler/jsonschemafriend.svg)](https://jitpack.io/#net.jimblackler/jsonschemafriend)

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
    implementation 'net.jimblackler:jsonschemafriend:0.9.3'
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
    <groupId>net.jimblackler</groupId>
    <artifactId>jsonschemafriend</artifactId>
    <version>0.9.3</version>
</dependency>
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20200518</version>
</dependency>
```

# Usage

Javadocs can be found
[here](https://javadoc.jitpack.io/com/github/jimblackler/jsonschematypes/jsonschemafriend/0.7.12/javadoc/net/jimblackler/jsonschemafriend/package-summary.html).

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
      Validator validator = new Validator();  // Create a validator.
      validator.validate(schema, 1); // Will not throw an exception.
      validator.validate(schema, "x"); // Will throw a ValidationException.
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

      Validator validator = new Validator();

      // Will not throw an exception.
      validator.validate(schema, Main.class.getResourceAsStream("/data1.json"));

      // Will throw a ValidationException.
      validator.validate(schema, Main.class.getResourceAsStream("/data2.json"));
    } catch (SchemaException | IOException e) {
      e.printStackTrace();
    }
  }
}
```

## From URIs or URLs.

This example loads both the schema, and the data to test from the internet, via
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

      URI resume = URI.create(
          "https://gist.githubusercontent.com/thomasdavis/c9dcfa1b37dec07fb2ee7f36d7278105/raw");
      // Will not throw an exception; document passes the schema.
      new Validator().validate(schema, resume);

    } catch (SchemaException | IOException e) {
      e.printStackTrace();
    }
  }
}
```

### From files.

Both schemas and test data can be specified as a `java.io.File`. For example:

```json
Schema schema = schemaStore.loadSchema(new File("/tmp/schema.json"));
new Validator().validate(schema, new File("/tmp/test.json"));
```

## Custom validation handling.

A custom `Consumer` can be passed to the validator to collect validation errors,
rather than triggering a `ValidationException`.

```java
import java.net.URI;
import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;
import org.json.JSONObject;

public class Main {
  public static void main(String[] args) {
    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      // Load the schema.
      Schema schema =
          schemaStore.loadSchema(URI.create("https://json.schemastore.org/chrome-manifest"));

      // Send an object that won't validate, and collect the validation errors.
      JSONObject document = new JSONObject();
      new Validator().validate(schema, document, validationError -> {
        if (validationError instanceof MissingPropertyError) {
          MissingPropertyError missingPropertyError = (MissingPropertyError) validationError;
          System.out.println("A missing property was: " + missingPropertyError.getProperty());
        }
      });
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```
