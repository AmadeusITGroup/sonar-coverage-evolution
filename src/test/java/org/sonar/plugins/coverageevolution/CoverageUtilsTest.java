package org.sonar.plugins.coverageevolution;

import org.junit.Test;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.coverageevolution.CoverageUtils.computeEffectiveKey;
import static org.sonar.plugins.coverageevolution.CoverageUtils.roundedPercentageGreaterThan;

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

  @Test
  public void testRounderPercentageGreaterThan() {
    assertTrue(roundedPercentageGreaterThan(1.1, 1.0));
    assertFalse(roundedPercentageGreaterThan(1.0, 1.0));
    assertFalse(roundedPercentageGreaterThan(0.9, 1.0));
    assertFalse(roundedPercentageGreaterThan(1.04, 1.0));
    assertTrue(roundedPercentageGreaterThan(1.05, 1.0));
  }

  @Test
  public void testComputeEffectiveKey() {
    Project module = new Project("foo:bar");
    assertEquals(
        "foo:bar",
        computeEffectiveKey(new Project("foo:bar"), module)
    );
    assertEquals(
        "baz",
        computeEffectiveKey(new Project("baz"), module)
    );

    Resource r = File.create("xyz");
    assertEquals(
        "foo:bar:xyz",
        computeEffectiveKey(r, module)
    );
    r.setEffectiveKey("quux");
    assertEquals(
        "quux",
        computeEffectiveKey(r, module)
    );
  }
}
