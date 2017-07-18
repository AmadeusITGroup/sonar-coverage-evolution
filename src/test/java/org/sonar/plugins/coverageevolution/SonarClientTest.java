package org.sonar.plugins.coverageevolution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import org.json.simple.DeserializationException;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;

public class SonarClientTest {
  @Test
  public void testMakeAuthHeader() {
    assertEquals(
        Optional.of("Basic Zm9vOmJhcg=="),
        SonarClient.makeAuthHeader("foo", "bar")
    );
  }

  @Test
  public void testExtractMeasure() throws Exception {
    JsonObject o = readJsonResource("componentMeasure.json");
    assertEquals(
        SonarClient.extractMeasure(o, CoreMetrics.COMPLEXITY),
        Optional.of("12")
    );

    assertEquals(
        SonarClient.extractMeasure(
            (JsonObject) Jsoner.deserialize("{}"),
            CoreMetrics.COMPLEXITY),
        Optional.empty()
    );
  }

  private JsonObject readJsonResource(String name) throws DeserializationException, IOException {
    return (JsonObject) Jsoner.deserialize(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(name)));
  }

  @Test
  public void testEncodeUrlPathComponent() {
    assertEquals(
        "foo%3Abar%20baz",
        SonarClient.encodeUrlPathComponent("foo:bar baz")
    );
  }

}
