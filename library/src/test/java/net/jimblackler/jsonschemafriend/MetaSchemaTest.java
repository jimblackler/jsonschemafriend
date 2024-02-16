package net.jimblackler.jsonschemafriend;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class MetaSchemaTest {

  /**
   * GitHub workflow that runs with this set to {@code true}.
   *
   * <p>These tests, for some reason, fail on GitHub with error 403 Forbidden.
   * Until we track down the cause, they are disabled on GitHub.
   */
  private static final boolean SMOKE_TEST = Boolean.getBoolean("run.smoke.test");

  @ParameterizedTest(name = "Meta schema test {0}")
  @MethodSource("provideSupportedMetaSchemas")
  public void testMetaSchema(URI uri) throws Exception {
    assumeFalse(SMOKE_TEST);
    SchemaStore schemaStore = new SchemaStore();
    Schema schema = schemaStore.loadSchema(uri);
    new Validator().validate(schema, uri);
  }

  static URI[] provideSupportedMetaSchemas() {
    return new URI[] {MetaSchemaUris.DRAFT_3, MetaSchemaUris.DRAFT_4, MetaSchemaUris.DRAFT_6,
        MetaSchemaUris.DRAFT_7, MetaSchemaUris.DRAFT_2019_09, MetaSchemaUris.DRAFT_2020_12};
  }
}
