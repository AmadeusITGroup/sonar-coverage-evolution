package org.sonar.plugins.coverageevolution.client;

import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public interface SonarClient {
  Double getMeasureValue(Project module, Resource resource, Metric<Double> metric);
}
