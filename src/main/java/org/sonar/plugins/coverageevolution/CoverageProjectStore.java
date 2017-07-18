package org.sonar.plugins.coverageevolution;

import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class CoverageProjectStore implements BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSensor.class);

  private int linesToCover = 0;
  private int uncoveredLines = 0;
  private boolean coverageRetrieved = false;

  public Double getProjectCoverage() {
    coverageRetrieved = true;
    return calculateCoverage(linesToCover, uncoveredLines);
  }

  public void updateMeasurements(int linesToCover, int uncoveredLines) {
    if (coverageRetrieved) {
      LOGGER.error(
          "Tried to update project-wide coverage data after the total has been calculated. " +
          "This is violates an assumption of the coverage evolution plugin. " +
          "Please report this issue."
      );
    }
    this.linesToCover += linesToCover;
    this.uncoveredLines += uncoveredLines;
  }
}
