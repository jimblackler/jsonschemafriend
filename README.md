# jsonschemafriend

jsonschemafriend is a JSON Schema loader and validator, delivered as a Java
library.

# About

An online demonstration
[is here](https://tryjsonschematypes.appspot.com/#validate).

It is written by <jimblackler@gmail.com> and offered under an
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

It is compatible with the following metaschemas

*   http://json-schema.org/draft-03/schema#
*   http://json-schema.org/draft-04/schema#
*   http://json-schema.org/draft-06/schema#
*   http://json-schema.org/draft-07/schema#
*   https://json-schema.org/draft/2019-09/schema
*   https://json-schema.org/draft/2020-12/schema

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

Add the project as a dependency in the module's `build.gradle`.

```groovy
dependencies {
    implementation 'net.jimblackler.jsonschemafriend:core:0.11.0'
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
    <version>0.10.6</version>
</dependency>
```

# Usage

Javadocs can be found
[here](https://javadoc.jitpack.io/com/github/jimblackler/jsonschematypes/jsonschemafriend/0.11.0/javadoc/net/jimblackler/jsonschemafriend/package-summary.html).

## Basic example using JSON strings

```java
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    // Create a new schema in a JSON string.
    String schemaString = "{"
        + "  \"$schema\": \"http://json-schema.org/draft-07/schema#\","
        + "  \"type\": \"integer\""
        + "}";

    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      Schema schema = schemaStore.loadSchemaJson(schemaString); // Load the schema.
      Validator validator = new Validator(); // Create a validator.
      validator.validateJson(schema, "1"); // Will not throw an exception.
      validator.validateJson(schema, "true"); // Will throw a ValidationException.
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```

## Via a Map

Schemas and objects can be provided in the form of standard Java objects. This
enables the selection of a JSON parser by the client based on preferences such
as speed, handling of numbers, and handling of key order, all of which vary
between libraries. Clients can also chose to construct these document directly
or on import from different formats such as JSON5 and YAML. It also makes it
easier to validate documents before serialization.

The parser takes documents and schemas as a tree of objects, typed as follows:

[JSON value][] | Java class
-------------- | -------------------------------
object         | `java.util.Map<String, Object>`
array          | `java.util.List<Object>`
number         | `java.lang.Number`
string         | `java.lang.String`
true/false     | `java.lang.Boolean`
null           | `null`

Documents arranged this way can be created by all major JSON libraries for Java,
including:

*   [org.json](https://mvnrepository.com/artifact/org.json/json)

    `new JSONObject(jsonString).toMap()`

*   [gson](https://github.com/google/gson)

    `new Gson().fromJson(jsonString, Map.class);`

*   [Jackson](https://github.com/FasterXML/jackson)

    `new ObjectMapper().readValue(jsonString, Map.class);`

*   [usejson](https://github.com/jimblackler/usejson)

    `new Json5Parser().parse(jsonString);`

This is an example of loading a schema in a `Map`.

```java
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

import java.util.HashMap;
import java.util.Map;

public class Main {
  public static void main(String[] args) {
    // Create a new schema in a map.
    Map<String, Object> schemaMap = new HashMap<>();
    schemaMap.put("$schema", "http://json-schema.org/draft-07/schema#");
    schemaMap.put("type", "integer");

    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      Schema schema = schemaStore.loadSchema(schemaMap); // Load the schema.
      Validator validator = new Validator(); // Create a validator.
      validator.validate(schema, 1); // Will not throw an exception.
      validator.validate(schema, "x"); // Will throw a ValidationException.
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```

## Via a JSONObject

This is an example of loading a schema in a `JSONObject` using
`JSONObject.toMap()`.

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
      Schema schema = schemaStore.loadSchema(schemaJson.toMap()); // Load the schema.
      Validator validator = new Validator(); // Create a validator.
      validator.validate(schema, 1); // Will not throw an exception.
      validator.validate(schema, "x"); // Will throw a ValidationException.
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```

## Via Java Resources

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

## From URIs or URLs

This example loads both the schema, and the data to test from the internet, via
URIs (URLs can also be used).

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

### From files

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
import java.util.HashMap;
import java.util.Map;

import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      // Load the schema.
      Schema schema =
          schemaStore.loadSchema(URI.create("https://json.schemastore.org/chrome-manifest"));

      // Send an object that won't validate, and collect the validation errors.
      Map<String, Object> document = new HashMap<>();
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

## Strict regular expression handling.

Strictly ECMA-compliant regular expressions requires an ECMA interpreter,
GraalJS. This is fairly heavyweight and is not compatible with Android. For this
mode include both the core library and the extra library, and initialize it as
shown in the fragment below.

```groovy
dependencies {
// ...
    implementation 'net.jimblackler.jsonschemafriend:core:0.10.5'
    implementation 'net.jimblackler.jsonschemafriend:extra:0.10.5'
}
```

```java
import net.jimblackler.jsonschemafriend.CachedRegExPatternSupplier;
import net.jimblackler.jsonschemafriend.Validator;
import net.jimblackler.jsonschemafriendextra.Ecma262Pattern;

public class Main {
  public static void main(String[] args) {
    Validator validator =
        new Validator(new CachedRegExPatternSupplier(Ecma262Pattern::new), validationError -> true);
  }
}
```

[JSON value]: https://tools.ietf.org/html/rfc7159#section-3
