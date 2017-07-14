package org.sonar.plugins.coverageevolution;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class CoveragePlugin extends SonarPlugin {

  @Override
  public List getExtensions() {
    return Arrays.asList(
        CoverageRule.class,
        CoverageSensor.class,
        CoverageProjectStore.class,
        CoverageConfiguration.class
    );
  }
}
