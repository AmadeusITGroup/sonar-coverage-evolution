package org.sonar.plugins.coverageevolution;

import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.coverageevolution.client.DefaultSonarClient;
import org.sonar.plugins.coverageevolution.client.SonarClient;

public class SonarClientComponent implements SonarClient {
  private SonarClient client;

  public SonarClientComponent(CoverageConfiguration config) {
    client = new DefaultSonarClient(config.url(), config.login(), config.password());
  }

  @Override
  public Double getMeasureValue(Project module, Resource resource, Metric<Double> metric) {
    return client.getMeasureValue(module, resource, metric);
  }
}
