package org.sonar.plugins.coverageevolution;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.coverageevolution.client.DefaultSonarClient;
import org.sonar.plugins.coverageevolution.client.SonarClient;

public class CoveragePlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    return Arrays.asList(
        CoverageRule.class,
        CoverageSensor.class,
        CoverageProjectStore.class,
        CoverageConfiguration.class,
        SonarClient.class,
        SonarClientComponent.class
    );
  }
}
