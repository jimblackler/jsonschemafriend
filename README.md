# jsonschemafriend

jsonschemafriend is a JSON Schema-based data validator, delivered as a Java
library.

As well as offering standards-compliant validation, it can provide JSON Schema
loading services to tools, allowing them to explore a correctly built schema
structure using typed accessors.

# About

An online demonstration
[is here](https://tryjsonschematypes.appspot.com/#validate).

It is written by <jimblackler@gmail.com> and offered under an
[Apache 2.0 license](https://www.apache.org/licenses/LICENSE-2.0).

It is compatible with the following versions of the standard.

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
    implementation 'net.jimblackler.jsonschemafriend:core:0.11.2'
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
    <groupId>net.jimblackler.jsonschemafriend</groupId>
    <artifactId>core</artifactId>
    <version>0.11.2</version>
</dependency>
```

# Usage

Javadocs can be found
[here](https://javadoc.jitpack.io/com/github/jimblackler/jsonschematypes/jsonschemafriend/0.11.2/javadoc/net/jimblackler/jsonschemafriend/package-summary.html).

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

```java
Schema schema = schemaStore.loadSchema(new File("/tmp/schema.json"));
new Validator().validate(schema, new File("/tmp/test.json"));
```

## Custom validation handling

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

## Validating formats 

Starting with Json Schema Draft 2019-09 validation of formats is an optional feature. Pass a `true` boolean to the Validator constructor to enable format validation.

```java
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    // Create a new schema in a JSON string.
    String schemaString = "{"
        + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
        + "  \"format\": \"uri\""
        + "}";

    try {
      SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
      Schema schema = schemaStore.loadSchemaJson(schemaString); // Load the schema.
      Validator validator = new Validator(true); // Create a validator for validating formats.
      validator.validateJson(schema, "\"https://foo.bar/?baz=qux#quux\""); // Will not throw an exception.
      validator.validateJson(schema, "\"bar,baz:foo\""); // Will throw a ValidationException.
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```

## Custom schema loading

By default the SchemaStore will use a class `CacheLoader` to resolve a schema URI. For http/https URIs this will download the schema and cache it locally for future use. A custom `Loader` can be passed to the SchemaStore to allow alternative methods for retrieving schemas.

```java
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import net.jimblackler.jsonschemafriend.Loader;
import net.jimblackler.jsonschemafriend.MissingPropertyError;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaException;
import net.jimblackler.jsonschemafriend.SchemaStore;
import net.jimblackler.jsonschemafriend.Validator;

public class Main {
  public static void main(String[] args) {
    try {
       // An inefficient example that connects to and loads a schema from a database.
      Loader databaseLoader = new Loader() {
        public String load(URI uri, boolean cacheSchema) throws IOException {
          try (
            Connection con = DriverManager.getConnection("...", "user", "pass");
            Statement stmt = con.createStatement();
            ResultSet resultSet = stmt.executeQuery("..."); // some query that uses the uri
          ) {
            resultSet.first();
            return resultSet.getString("schema");
          } catch (SQLException e) {
            throw new IOException("Unable to retrieve schema", e);
          }
        }
      });
    
      SchemaStore schemaStore = new SchemaStore(databaseLoader);
      // Load the schema.
      Schema schema =
          schemaStore.loadSchema(URI.create("https://json.schemastore.org/chrome-manifest"));
    } catch (SchemaException e) {
      e.printStackTrace();
    }
  }
}
```


## As a parser

The library can act as a parser for applications that need to work with JSON
Schemas. For example; code creation tools, test data generators, schema
converters or visualizers.

It offers typed accessors for Schema keywords but the main value it offers is to
build a correctly connected Schema tree. Although JSON Schemas are plain JSON
objects, locating subschemas from $ref is not trivial. Keywords $id, $anchor and
$ref require careful handling, and requirements differ across JSON Schema
standard versions. The library can shield clients from these details.

Once a Schema has been loaded, it can be evaluated with accessors in the Schema
class, for example:

```java
import net.jimblackler.jsonschemafriend.GenerationException;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;

import java.net.URI;
import java.util.Map;

public class Main {
  public static void main(String[] args) throws GenerationException {
    SchemaStore schemaStore = new SchemaStore(); // Initialize a SchemaStore.
    // Load the schema.
    Schema schema =
        schemaStore.loadSchema(URI.create("https://json.schemastore.org/mocharc.json"));

    // Display the metaschema, for example http://json-schema.org/draft-07/schema#
    URI metaSchema = schema.getMetaSchema();
    System.out.println(metaSchema);

    // Get the 'color' property and print its canonical URI and its resource URI (where it can be
    // found in the schema document).
    Schema color = schema.getProperties().get("color");
    System.out.println(color.getExplicitTypes());  // [boolean]
    System.out.println(color.getUri());  // https://json.schemastore.org/mocharc#/definitions/bool
    System.out.println(
        color.getResourceUri());  // https://json.schemastore.org/mocharc.json#/definitions/bool

    // Display the URIs of all the subschemas (immediate dependents of the schema).
    Map<URI, Schema> subSchemas = schema.getSubSchemas();
    for (URI uri: subSchemas.keySet()) {
      System.out.println(uri);
    }
  }
}
```

[JSON value]: https://tools.ietf.org/html/rfc7159#section-3
