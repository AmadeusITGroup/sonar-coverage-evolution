package org.sonar.plugins.coverageevolution;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;

public class CoverageUtilsTest {

  private static final double DELTA = 1e-6;

  @Test
  public void testCalculateCoverage() {
    assertEquals(100.0, calculateCoverage(0, 0), DELTA); // special case
    assertEquals(50.0, calculateCoverage(100, 50), DELTA);
    assertEquals(94.5, calculateCoverage(1000, 55), DELTA);
    assertEquals(87.3, calculateCoverage(1000, 127), DELTA);
    assertEquals(31.09, calculateCoverage(10000, 6891), DELTA);
    assertEquals(0.27, calculateCoverage(10000, 9973), DELTA);

    // not a wanted case but it works with current implementation (so good future exception test)
    assertEquals(200, calculateCoverage(100, -100), DELTA);
  }
}
