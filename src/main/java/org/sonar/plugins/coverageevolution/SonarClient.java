package org.sonar.plugins.coverageevolution;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.json.simple.DeserializationException;
import org.json.simple.JsonArray;
import org.json.simple.JsonObject;
import org.json.simple.Jsoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public class SonarClient {
  private String url;
  private String login;
  private String password;

  private static final Logger LOG = LoggerFactory.getLogger(SonarClient.class);

  public SonarClient(String url, String login, String password) {
    this.url = url;
    this.login = login;
    this.password = password;
  }

  public Double getMeasureValue(Project module, Resource resource, Metric<Double> metric) {
    return getMeasureValueInt(module, resource, metric).map(Double::valueOf).orElse(null);
  }

  private <G extends Serializable> Optional<String> getMeasureValueInt(Project module, Resource resource, Metric<G> metric) {
    URL apiUrl = null;
    String effectiveKey = CoverageUtils.computeEffectiveKey(resource, module);
    try {
      apiUrl = new URL(
          url +
              "/api/measures/component?" +
              "componentKey=" + encodeUrlPathComponent(effectiveKey) +
              "&" +
              "metricKeys=" + encodeUrlPathComponent(metric.getKey())
      );
      LOG.debug("Fetching {}", apiUrl);
      HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
      connection.setRequestProperty("Accept", "application/json");
      Optional<String> authHeader = makeAuthHeader(login, password);
      if (authHeader.isPresent()) {
        connection.setRequestProperty("Authorization", authHeader.get());
      }
      connection.setDoOutput(false);
      connection.setDoInput(true);
      JsonObject response = (JsonObject) Jsoner.deserialize(new InputStreamReader(
          connection.getInputStream()
      ));
      return extractMeasure(response, metric);
    } catch (IOException | DeserializationException e) {
      LOG.error("Could not fetch measure {} for {}: {}",
          metric.getKey(), resource.getEffectiveKey(), e.getMessage(), e);
      return Optional.empty();
    }
  }

  protected static <G extends Serializable> Optional<String> extractMeasure(JsonObject apiResult, Metric<G> metric) {
    JsonObject component = apiResult.getMap("component");
    if (component == null) {
      return Optional.empty();
    }
    JsonArray measures = component.getCollection("measures");
    if (measures == null) {
      return Optional.empty();
    }
    return measures.stream()
        .map(o -> (JsonObject) o)
        .filter(o ->
          metric.getKey().equals(o.getString("metric"))
        )
        .map(o ->
          o.getString("value")
        )
        .findFirst();

  }

  protected static Optional<String> makeAuthHeader(String login, String password) {
    if (login == null && password == null) {
      return Optional.empty();
    }

    return Optional.of("Basic " + Base64.getEncoder().encodeToString(
        (
            login + ":" + (password == null ? "" : password)
        ).getBytes(StandardCharsets.ISO_8859_1)
    ));
  }

  protected static String encodeUrlPathComponent(String c) {
    try {
      return URLEncoder.encode(c, StandardCharsets.UTF_8.displayName())
          .replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      // We actually use an supported encoding...
      throw new UnreachableCodeException("This should not have happened");
    }
  }

  private static class UnreachableCodeException extends RuntimeException {
    public UnreachableCodeException(
        String s) {
      super(s);
    }
  }
}
