package org.sonar.plugins.coverageevolution;

import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;

import org.sonar.api.BatchComponent;
import org.sonar.api.batch.InstantiationStrategy;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class CoverageProjectStore implements BatchComponent {

  private int linesToCover = 0;
  private int uncoveredLines = 0;

  public Double getProjectCoverage() {
    return calculateCoverage(linesToCover, uncoveredLines);
  }

  public void updateMeasurements(int linesToCover, int uncoveredLines) {
    this.linesToCover += linesToCover;
    this.uncoveredLines += uncoveredLines;
  }
}
