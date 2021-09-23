package net.jimblackler.jsonschemafriend;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MetaSchemaTest {
  @ParameterizedTest(name = "Meta schema test {0}")
  @MethodSource("provideSupportedMetaSchemas")
  public void testMetaSchema(URI uri) throws Exception {
    SchemaStore schemaStore = new SchemaStore();
    Schema schema = schemaStore.loadSchema(uri);
    new Validator().validate(schema, uri);
  }

  static URI[] provideSupportedMetaSchemas() {
    return new URI[] {MetaSchemaUris.DRAFT_3, MetaSchemaUris.DRAFT_4, MetaSchemaUris.DRAFT_6,
        MetaSchemaUris.DRAFT_7, MetaSchemaUris.DRAFT_2019_09, MetaSchemaUris.DRAFT_2020_12};
  }
}
