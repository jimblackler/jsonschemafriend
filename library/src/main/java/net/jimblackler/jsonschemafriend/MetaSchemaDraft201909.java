package net.jimblackler.jsonschemafriend;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MetaSchemaDraft201909 {
    private static final String[] SCHEMA_JSONS = new String[] {
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/core\": true,\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/applicator\": true,\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/validation\": true,\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/meta-data\": true,\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/format\": false,\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/content\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Core and Validation specifications meta-schema\",\n" +
                    "  \"allOf\": [\n" +
                    "    {\"$ref\": \"meta/core\"},\n" +
                    "    {\"$ref\": \"meta/applicator\"},\n" +
                    "    {\"$ref\": \"meta/validation\"},\n" +
                    "    {\"$ref\": \"meta/meta-data\"},\n" +
                    "    {\"$ref\": \"meta/format\"},\n" +
                    "    {\"$ref\": \"meta/content\"}\n" +
                    "  ],\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"definitions\": {\n" +
                    "      \"$comment\": \"While no longer an official keyword as it is replaced by $defs, this keyword is retained in the meta-schema to prevent incompatible extensions as it remains in common use.\",\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"dependencies\": {\n" +
                    "      \"$comment\": \"\\\"dependencies\\\" is no longer a keyword, but schema authors should avoid redefining it to facilitate a smooth transition to \\\"dependentSchemas\\\" and \\\"dependentRequired\\\"\",\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"anyOf\": [{\"$recursiveRef\": \"#\"}, {\"$ref\": \"meta/validation#/$defs/stringArray\"}]\n" +
                    "      }\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/applicator\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/applicator\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Applicator vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"additionalItems\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"unevaluatedItems\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"items\": {\n" +
                    "      \"anyOf\": [{\"$recursiveRef\": \"#\"}, {\"$ref\": \"#/$defs/schemaArray\"}]\n" +
                    "    },\n" +
                    "    \"contains\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"additionalProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"unevaluatedProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"properties\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"patternProperties\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "      \"propertyNames\": {\"format\": \"regex\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"dependentSchemas\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"$recursiveRef\": \"#\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"propertyNames\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"if\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"then\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"else\": {\"$recursiveRef\": \"#\"},\n" +
                    "    \"allOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"anyOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"oneOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"not\": {\"$recursiveRef\": \"#\"}\n" +
                    "  },\n" +
                    "  \"$defs\": {\n" +
                    "    \"schemaArray\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"minItems\": 1,\n" +
                    "      \"items\": {\"$recursiveRef\": \"#\"}\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/content\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/content\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Content vocabulary meta-schema\",\n" +
                    "\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"contentMediaType\": {\"type\": \"string\"},\n" +
                    "    \"contentEncoding\": {\"type\": \"string\"},\n" +
                    "    \"contentSchema\": {\"$recursiveRef\": \"#\"}\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/core\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/core\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Core vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"$id\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri-reference\",\n" +
                    "      \"$comment\": \"Non-empty fragments not allowed.\",\n" +
                    "      \"pattern\": \"^[^#]*#?$\"\n" +
                    "    },\n" +
                    "    \"$schema\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri\"\n" +
                    "    },\n" +
                    "    \"$anchor\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"pattern\": \"^[A-Za-z][-A-Za-z0-9.:_]*$\"\n" +
                    "    },\n" +
                    "    \"$ref\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri-reference\"\n" +
                    "    },\n" +
                    "    \"$recursiveRef\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri-reference\"\n" +
                    "    },\n" +
                    "    \"$recursiveAnchor\": {\n" +
                    "      \"type\": \"boolean\",\n" +
                    "      \"default\": false\n" +
                    "    },\n" +
                    "    \"$vocabulary\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"propertyNames\": {\n" +
                    "        \"type\": \"string\",\n" +
                    "        \"format\": \"uri\"\n" +
                    "      },\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"type\": \"boolean\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"$comment\": {\n" +
                    "      \"type\": \"string\"\n" +
                    "    },\n" +
                    "    \"$defs\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$recursiveRef\": \"#\"},\n" +
                    "      \"default\": {}\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/format\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/format\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Format vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"format\": {\"type\": \"string\"}\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/meta-data\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/meta-data\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Meta-data vocabulary meta-schema\",\n" +
                    "\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"title\": {\n" +
                    "      \"type\": \"string\"\n" +
                    "    },\n" +
                    "    \"description\": {\n" +
                    "      \"type\": \"string\"\n" +
                    "    },\n" +
                    "    \"default\": true,\n" +
                    "    \"deprecated\": {\n" +
                    "      \"type\": \"boolean\",\n" +
                    "      \"default\": false\n" +
                    "    },\n" +
                    "    \"readOnly\": {\n" +
                    "      \"type\": \"boolean\",\n" +
                    "      \"default\": false\n" +
                    "    },\n" +
                    "    \"writeOnly\": {\n" +
                    "      \"type\": \"boolean\",\n" +
                    "      \"default\": false\n" +
                    "    },\n" +
                    "    \"examples\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"items\": true\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2019-09/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2019-09/meta/validation\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2019-09/vocab/validation\": true\n" +
                    "  },\n" +
                    "  \"$recursiveAnchor\": true,\n" +
                    "\n" +
                    "  \"title\": \"Validation vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"multipleOf\": {\n" +
                    "      \"type\": \"number\",\n" +
                    "      \"exclusiveMinimum\": 0\n" +
                    "    },\n" +
                    "    \"maximum\": {\n" +
                    "      \"type\": \"number\"\n" +
                    "    },\n" +
                    "    \"exclusiveMaximum\": {\n" +
                    "      \"type\": \"number\"\n" +
                    "    },\n" +
                    "    \"minimum\": {\n" +
                    "      \"type\": \"number\"\n" +
                    "    },\n" +
                    "    \"exclusiveMinimum\": {\n" +
                    "      \"type\": \"number\"\n" +
                    "    },\n" +
                    "    \"maxLength\": {\"$ref\": \"#/$defs/nonNegativeInteger\"},\n" +
                    "    \"minLength\": {\"$ref\": \"#/$defs/nonNegativeIntegerDefault0\"},\n" +
                    "    \"pattern\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"regex\"\n" +
                    "    },\n" +
                    "    \"maxItems\": {\"$ref\": \"#/$defs/nonNegativeInteger\"},\n" +
                    "    \"minItems\": {\"$ref\": \"#/$defs/nonNegativeIntegerDefault0\"},\n" +
                    "    \"uniqueItems\": {\n" +
                    "      \"type\": \"boolean\",\n" +
                    "      \"default\": false\n" +
                    "    },\n" +
                    "    \"maxContains\": {\"$ref\": \"#/$defs/nonNegativeInteger\"},\n" +
                    "    \"minContains\": {\n" +
                    "      \"$ref\": \"#/$defs/nonNegativeInteger\",\n" +
                    "      \"default\": 1\n" +
                    "    },\n" +
                    "    \"maxProperties\": {\"$ref\": \"#/$defs/nonNegativeInteger\"},\n" +
                    "    \"minProperties\": {\"$ref\": \"#/$defs/nonNegativeIntegerDefault0\"},\n" +
                    "    \"required\": {\"$ref\": \"#/$defs/stringArray\"},\n" +
                    "    \"dependentRequired\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"$ref\": \"#/$defs/stringArray\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"const\": true,\n" +
                    "    \"enum\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"items\": true\n" +
                    "    },\n" +
                    "    \"type\": {\n" +
                    "      \"anyOf\": [\n" +
                    "        {\"$ref\": \"#/$defs/simpleTypes\"},\n" +
                    "        {\n" +
                    "          \"type\": \"array\",\n" +
                    "          \"items\": {\"$ref\": \"#/$defs/simpleTypes\"},\n" +
                    "          \"minItems\": 1,\n" +
                    "          \"uniqueItems\": true\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"$defs\": {\n" +
                    "    \"nonNegativeInteger\": {\n" +
                    "      \"type\": \"integer\",\n" +
                    "      \"minimum\": 0\n" +
                    "    },\n" +
                    "    \"nonNegativeIntegerDefault0\": {\n" +
                    "      \"$ref\": \"#/$defs/nonNegativeInteger\",\n" +
                    "      \"default\": 0\n" +
                    "    },\n" +
                    "    \"simpleTypes\": {\n" +
                    "      \"enum\": [\"array\", \"boolean\", \"integer\", \"null\", \"number\", \"object\", \"string\"]\n" +
                    "    },\n" +
                    "    \"stringArray\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"items\": {\"type\": \"string\"},\n" +
                    "      \"uniqueItems\": true,\n" +
                    "      \"default\": []\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
    };

    static final List<Object> SCHEMAS;

    static {
        List<Object> schemaObjects;
        try {
            schemaObjects = Arrays.stream(SCHEMA_JSONS).map(j -> {
                try {
                    return new ObjectMapper().readValue(j, Object.class);
                }
                catch (Throwable th) {
                    throw new RuntimeException(th);
                }
            }).collect(Collectors.toList());
        }
        catch (Throwable ignored) {
            schemaObjects = null;
        }
        SCHEMAS = schemaObjects;
    }
}
