package net.jimblackler.jsonschemafriend;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class MetaSchemaDraft202012 {
    private static final String[] SCHEMA_JSONS = new String[] {
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/core\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/applicator\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/unevaluated\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/validation\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/meta-data\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/format-annotation\": true,\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/content\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Core and Validation specifications meta-schema\",\n" +
                    "  \"allOf\": [\n" +
                    "    {\"$ref\": \"meta/core\"},\n" +
                    "    {\"$ref\": \"meta/applicator\"},\n" +
                    "    {\"$ref\": \"meta/unevaluated\"},\n" +
                    "    {\"$ref\": \"meta/validation\"},\n" +
                    "    {\"$ref\": \"meta/meta-data\"},\n" +
                    "    {\"$ref\": \"meta/format-annotation\"},\n" +
                    "    {\"$ref\": \"meta/content\"}\n" +
                    "  ],\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"$comment\": \"This meta-schema also defines keywords that have appeared in previous drafts in order to prevent incompatible extensions as they remain in common use.\",\n" +
                    "  \"properties\": {\n" +
                    "    \"definitions\": {\n" +
                    "      \"$comment\": \"\\\"definitions\\\" has been replaced by \\\"$defs\\\".\",\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "      \"deprecated\": true,\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"dependencies\": {\n" +
                    "      \"$comment\": \"\\\"dependencies\\\" has been split and replaced by \\\"dependentSchemas\\\" and \\\"dependentRequired\\\" in order to serve their differing semantics.\",\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"anyOf\": [{\"$dynamicRef\": \"#meta\"}, {\"$ref\": \"meta/validation#/$defs/stringArray\"}]\n" +
                    "      },\n" +
                    "      \"deprecated\": true,\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"$recursiveAnchor\": {\n" +
                    "      \"$comment\": \"\\\"$recursiveAnchor\\\" has been replaced by \\\"$dynamicAnchor\\\".\",\n" +
                    "      \"$ref\": \"meta/core#/$defs/anchorString\",\n" +
                    "      \"deprecated\": true\n" +
                    "    },\n" +
                    "    \"$recursiveRef\": {\n" +
                    "      \"$comment\": \"\\\"$recursiveRef\\\" has been replaced by \\\"$dynamicRef\\\".\",\n" +
                    "      \"$ref\": \"meta/core#/$defs/uriReferenceString\",\n" +
                    "      \"deprecated\": true\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/applicator\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/applicator\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Applicator vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"prefixItems\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"items\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"contains\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"additionalProperties\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"properties\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"patternProperties\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "      \"propertyNames\": {\"format\": \"regex\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"dependentSchemas\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "      \"default\": {}\n" +
                    "    },\n" +
                    "    \"propertyNames\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"if\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"then\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"else\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"allOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"anyOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"oneOf\": {\"$ref\": \"#/$defs/schemaArray\"},\n" +
                    "    \"not\": {\"$dynamicRef\": \"#meta\"}\n" +
                    "  },\n" +
                    "  \"$defs\": {\n" +
                    "    \"schemaArray\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"minItems\": 1,\n" +
                    "      \"items\": {\"$dynamicRef\": \"#meta\"}\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/content\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/content\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Content vocabulary meta-schema\",\n" +
                    "\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"contentEncoding\": {\"type\": \"string\"},\n" +
                    "    \"contentMediaType\": {\"type\": \"string\"},\n" +
                    "    \"contentSchema\": {\"$dynamicRef\": \"#meta\"}\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/core\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/core\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Core vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"$id\": {\n" +
                    "      \"$ref\": \"#/$defs/uriReferenceString\",\n" +
                    "      \"$comment\": \"Non-empty fragments not allowed.\",\n" +
                    "      \"pattern\": \"^[^#]*#?$\"\n" +
                    "    },\n" +
                    "    \"$schema\": {\"$ref\": \"#/$defs/uriString\"},\n" +
                    "    \"$ref\": {\"$ref\": \"#/$defs/uriReferenceString\"},\n" +
                    "    \"$anchor\": {\"$ref\": \"#/$defs/anchorString\"},\n" +
                    "    \"$dynamicRef\": {\"$ref\": \"#/$defs/uriReferenceString\"},\n" +
                    "    \"$dynamicAnchor\": {\"$ref\": \"#/$defs/anchorString\"},\n" +
                    "    \"$vocabulary\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"propertyNames\": {\"$ref\": \"#/$defs/uriString\"},\n" +
                    "      \"additionalProperties\": {\n" +
                    "        \"type\": \"boolean\"\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"$comment\": {\n" +
                    "      \"type\": \"string\"\n" +
                    "    },\n" +
                    "    \"$defs\": {\n" +
                    "      \"type\": \"object\",\n" +
                    "      \"additionalProperties\": {\"$dynamicRef\": \"#meta\"}\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"$defs\": {\n" +
                    "    \"anchorString\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"pattern\": \"^[A-Za-z_][-A-Za-z0-9._]*$\"\n" +
                    "    },\n" +
                    "    \"uriString\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri\"\n" +
                    "    },\n" +
                    "    \"uriReferenceString\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"format\": \"uri-reference\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/format-annotation\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/format-annotation\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Format vocabulary meta-schema for annotation results\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"format\": {\"type\": \"string\"}\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/meta-data\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/meta-data\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
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
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/unevaluated\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/unevaluated\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Unevaluated applicator vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
                    "    \"unevaluatedItems\": {\"$dynamicRef\": \"#meta\"},\n" +
                    "    \"unevaluatedProperties\": {\"$dynamicRef\": \"#meta\"}\n" +
                    "  }\n" +
                    "}\n",
            "{\n" +
                    "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n" +
                    "  \"$id\": \"https://json-schema.org/draft/2020-12/meta/validation\",\n" +
                    "  \"$vocabulary\": {\n" +
                    "    \"https://json-schema.org/draft/2020-12/vocab/validation\": true\n" +
                    "  },\n" +
                    "  \"$dynamicAnchor\": \"meta\",\n" +
                    "\n" +
                    "  \"title\": \"Validation vocabulary meta-schema\",\n" +
                    "  \"type\": [\"object\", \"boolean\"],\n" +
                    "  \"properties\": {\n" +
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
                    "    },\n" +
                    "    \"const\": true,\n" +
                    "    \"enum\": {\n" +
                    "      \"type\": \"array\",\n" +
                    "      \"items\": true\n" +
                    "    },\n" +
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
                    "}\n"
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
