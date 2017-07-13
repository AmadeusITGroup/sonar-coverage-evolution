package org.sonar.plugins.coverageevolution;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.sonar.api.batch.rule.ActiveRules;


public class CoverageProjectStoreTest {

  private static final double DELTA = 1e-6;

  private CoverageConfiguration SPC = mock(CoverageConfiguration.class);
  private ActiveRules AR = mock(ActiveRules.class);
  private CoverageProjectStore CoProSto = new CoverageProjectStore();


  @Test
  public void testGetProjectCoverage() {
    assertEquals(100.0, CoProSto.getProjectCoverage(), DELTA);
  }

  @Test
  public void testUpdateMeasurements() {

    CoProSto.updateMeasurements(37, 13);  // Little-endian leet ;o))

    assertEquals(64.86486486486487, CoProSto.getProjectCoverage(), DELTA);
  }
}
