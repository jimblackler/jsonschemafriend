package net.jimblackler.jsonschemafriend;

import com.fasterxml.jackson.databind.ObjectMapper;

class MetaSchemaDraft03 {
    private static final String SCHEMA_JSON = "{\n" +
            "\t\"$schema\" : \"http://json-schema.org/draft-03/schema#\",\n" +
            "\t\"id\" : \"http://json-schema.org/draft-03/schema#\",\n" +
            "\t\"type\" : \"object\",\n" +
            "\t\n" +
            "\t\"properties\" : {\n" +
            "\t\t\"type\" : {\n" +
            "\t\t\t\"type\" : [\"string\", \"array\"],\n" +
            "\t\t\t\"items\" : {\n" +
            "\t\t\t\t\"type\" : [\"string\", {\"$ref\" : \"#\"}]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"uniqueItems\" : true,\n" +
            "\t\t\t\"default\" : \"any\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"properties\" : {\n" +
            "\t\t\t\"type\" : \"object\",\n" +
            "\t\t\t\"additionalProperties\" : {\"$ref\" : \"#\"},\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"patternProperties\" : {\n" +
            "\t\t\t\"type\" : \"object\",\n" +
            "\t\t\t\"additionalProperties\" : {\"$ref\" : \"#\"},\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"additionalProperties\" : {\n" +
            "\t\t\t\"type\" : [{\"$ref\" : \"#\"}, \"boolean\"],\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"items\" : {\n" +
            "\t\t\t\"type\" : [{\"$ref\" : \"#\"}, \"array\"],\n" +
            "\t\t\t\"items\" : {\"$ref\" : \"#\"},\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"additionalItems\" : {\n" +
            "\t\t\t\"type\" : [{\"$ref\" : \"#\"}, \"boolean\"],\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"required\" : {\n" +
            "\t\t\t\"type\" : \"boolean\",\n" +
            "\t\t\t\"default\" : false\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"dependencies\" : {\n" +
            "\t\t\t\"type\" : \"object\",\n" +
            "\t\t\t\"additionalProperties\" : {\n" +
            "\t\t\t\t\"type\" : [\"string\", \"array\", {\"$ref\" : \"#\"}],\n" +
            "\t\t\t\t\"items\" : {\n" +
            "\t\t\t\t\t\"type\" : \"string\"\n" +
            "\t\t\t\t}\n" +
            "\t\t\t},\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"minimum\" : {\n" +
            "\t\t\t\"type\" : \"number\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"maximum\" : {\n" +
            "\t\t\t\"type\" : \"number\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"exclusiveMinimum\" : {\n" +
            "\t\t\t\"type\" : \"boolean\",\n" +
            "\t\t\t\"default\" : false\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"exclusiveMaximum\" : {\n" +
            "\t\t\t\"type\" : \"boolean\",\n" +
            "\t\t\t\"default\" : false\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"minItems\" : {\n" +
            "\t\t\t\"type\" : \"integer\",\n" +
            "\t\t\t\"minimum\" : 0,\n" +
            "\t\t\t\"default\" : 0\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"maxItems\" : {\n" +
            "\t\t\t\"type\" : \"integer\",\n" +
            "\t\t\t\"minimum\" : 0\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"uniqueItems\" : {\n" +
            "\t\t\t\"type\" : \"boolean\",\n" +
            "\t\t\t\"default\" : false\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"pattern\" : {\n" +
            "\t\t\t\"type\" : \"string\",\n" +
            "\t\t\t\"format\" : \"regex\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"minLength\" : {\n" +
            "\t\t\t\"type\" : \"integer\",\n" +
            "\t\t\t\"minimum\" : 0,\n" +
            "\t\t\t\"default\" : 0\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"maxLength\" : {\n" +
            "\t\t\t\"type\" : \"integer\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"enum\" : {\n" +
            "\t\t\t\"type\" : \"array\",\n" +
            "\t\t\t\"minItems\" : 1,\n" +
            "\t\t\t\"uniqueItems\" : true\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"default\" : {\n" +
            "\t\t\t\"type\" : \"any\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"title\" : {\n" +
            "\t\t\t\"type\" : \"string\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"description\" : {\n" +
            "\t\t\t\"type\" : \"string\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"format\" : {\n" +
            "\t\t\t\"type\" : \"string\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"divisibleBy\" : {\n" +
            "\t\t\t\"type\" : \"number\",\n" +
            "\t\t\t\"minimum\" : 0,\n" +
            "\t\t\t\"exclusiveMinimum\" : true,\n" +
            "\t\t\t\"default\" : 1\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"disallow\" : {\n" +
            "\t\t\t\"type\" : [\"string\", \"array\"],\n" +
            "\t\t\t\"items\" : {\n" +
            "\t\t\t\t\"type\" : [\"string\", {\"$ref\" : \"#\"}]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"uniqueItems\" : true\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"extends\" : {\n" +
            "\t\t\t\"type\" : [{\"$ref\" : \"#\"}, \"array\"],\n" +
            "\t\t\t\"items\" : {\"$ref\" : \"#\"},\n" +
            "\t\t\t\"default\" : {}\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"id\" : {\n" +
            "\t\t\t\"type\" : \"string\",\n" +
            "\t\t\t\"format\" : \"uri\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"$ref\" : {\n" +
            "\t\t\t\"type\" : \"string\",\n" +
            "\t\t\t\"format\" : \"uri\"\n" +
            "\t\t},\n" +
            "\t\t\n" +
            "\t\t\"$schema\" : {\n" +
            "\t\t\t\"type\" : \"string\",\n" +
            "\t\t\t\"format\" : \"uri\"\n" +
            "\t\t}\n" +
            "\t},\n" +
            "\t\n" +
            "\t\"dependencies\" : {\n" +
            "\t\t\"exclusiveMinimum\" : \"minimum\",\n" +
            "\t\t\"exclusiveMaximum\" : \"maximum\"\n" +
            "\t},\n" +
            "\t\n" +
            "\t\"default\" : {}\n" +
            "}";

    static final Object SCHEMA;

    static {
        Object schemaObject;
        try {
            schemaObject = new ObjectMapper().readValue(SCHEMA_JSON, Object.class);
        }
        catch (Throwable ignored) {
            schemaObject = null;
        }
        SCHEMA = schemaObject;
    }
}
