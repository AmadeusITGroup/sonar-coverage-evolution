package org.sonar.plugins.coverageevolution;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.sonar.plugins.coverageevolution.CoverageRule.decreasingLineCoverageRule;

public class CoverageRuleTest {

  @Test
  public void testDecreasingLineCoverageRule() {
    assertEquals("coverageEvolution-neutral:decreasingLineCoverage", decreasingLineCoverageRule("neutral").toString());
  }

  @Test
  public void testIsDecreasingLineCoverage() {
    assertFalse(isDecreasingLineCoverage("foo:bar"));
    assertFalse(isDecreasingLineCoverage("xxx-neutral:decreasingLineCoverage"));
    assertFalse(isDecreasingLineCoverage("coverageEvolution-neutral:xxx"));

    assertTrue(isDecreasingLineCoverage("coverageEvolution-neutral:decreasingLineCoverage"));
    assertTrue(isDecreasingLineCoverage("coverageEvolution-java:decreasingLineCoverage"));
  }

  private boolean isDecreasingLineCoverage(String s) {
    return CoverageRule.isDecreasingLineCoverage(RuleKey.parse(s));
  }


}
