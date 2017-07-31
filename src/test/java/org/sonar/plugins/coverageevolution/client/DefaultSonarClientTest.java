package org.sonar.plugins.coverageevolution.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
import org.json.simple.DeserializationException;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.coverageevolution.client.DefaultSonarClient;

public class DefaultSonarClientTest {
  @Rule
  public WireMockRule httpServer = new WireMockRule();

  @Test
  public void testMakeAuthHeader() {
    assertEquals(
        Optional.of("Basic Zm9vOmJhcg=="),
        DefaultSonarClient.makeAuthHeader("foo", "bar")
    );
    assertEquals(
        Optional.empty(),
        DefaultSonarClient.makeAuthHeader(null, null)
    );
    assertEquals(
        Optional.of("Basic Zm9vOg=="),
        DefaultSonarClient.makeAuthHeader("foo", null)
    );
  }

  @Test
  public void testExtractMeasure() throws Exception {
    JsonObject o = readJsonResource("componentMeasure.json");
    assertEquals(
        DefaultSonarClient.extractMeasure(o, CoreMetrics.COMPLEXITY),
        Optional.of("12")
    );

    assertEquals(
        DefaultSonarClient.extractMeasure(
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
        DefaultSonarClient.encodeUrlPathComponent("foo:bar baz")
    );
  }

  @Test
  public void testGetMeasureValue() throws Exception {
    String url = "http://localhost:" + httpServer.port() + "/";
    String username = "foo";
    String password = "bar";
    SonarClient client = new DefaultSonarClient(url, username, password);

    Project project = new Project("MY_PROJECT");
    Resource file = new File("ElementImpl.java");
    file.setKey("ElementImpl.java");
    httpServer.stubFor(
        get("/api/measures/component?componentKey=MY_PROJECT%3AElementImpl.java&metricKeys=line_coverage")
        .withBasicAuth(username, password)
        .willReturn(
            aResponse()
            .withBody(readJsonResource("componentMeasure.json").toJson().getBytes())
        )
    );
    assertEquals(
        25.0,
        client.getMeasureValue(project, file, CoreMetrics.LINE_COVERAGE),
        0.001
    );
  }

}
