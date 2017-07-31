package org.sonar.plugins.coverageevolution;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.Context;
import org.sonar.api.server.rule.RulesDefinition.Repository;
import org.sonar.api.server.rule.RulesDefinition.Rule;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class CoverageRuleTest {

  @Test
  public void testDecreasingLineCoverageRule() {
    assertEquals("coverageEvolution-neutral:decreasingLineCoverage", CoverageRule.decreasingLineCoverageRule("neutral").toString());
    assertEquals("coverageEvolution-java:decreasingLineCoverage", CoverageRule.decreasingLineCoverageRule("java").toString());
  }

  @Test
  public void testShouldExecute() {
    ActiveRulesBuilder builder = new ActiveRulesBuilder();

    assertFalse(CoverageRule.shouldExecute(builder.build()));

    builder = builder.create(RuleKey.parse("some:thing")).activate();
    assertFalse(CoverageRule.shouldExecute(builder.build()));

    builder = builder.create(CoverageRule.decreasingLineCoverageRule("java")).activate();
    assertTrue(CoverageRule.shouldExecute(builder.build()));

    builder = builder.create(CoverageRule.decreasingLineCoverageRule("neutral")).activate();
    assertTrue(CoverageRule.shouldExecute(builder.build()));
  }

  @Test
  public void testDefineBasic() {
    Languages languages = new Languages(testLanguage("java"));
    CoverageRule rule = new CoverageRule(languages);
    RulesDefinition.Context context = new Context();

    assertTrue(context.repositories().isEmpty());
    rule.define(context);
    List<Repository> repos = context.repositories();
    assertEquals(1, repos.size());

    Repository repo = repos.get(0);
    assertEquals("Coverage evolution", repo.name());
    List<Rule> rules = repo.rules();
    assertEquals(2, rules.size());
  }

  @Test
  public void testDefineWithMultipleLanguages() {
    Languages languages = new Languages(
        testLanguage("foo"),
        testLanguage("bar")
    );
    CoverageRule rule = new CoverageRule(languages);

    RulesDefinition.Context context = new Context();
    rule.define(context);
    assertEquals(2, context.repositories().size());
  }

  @Test
  public void testDecreasingOverallCoverageRule() {
    DefaultFileSystem fs = new DefaultFileSystem(null);
    fs.addLanguages("c", "java", "xml");

    fs.add(new DefaultInputFile("a.java").setAbsolutePath("a.java").setLanguage("java").setLines(10));
    fs.add(new DefaultInputFile("b.java").setAbsolutePath("b.java").setLanguage("java").setLines(15));
    fs.add(new DefaultInputFile("foo.c").setAbsolutePath("foo.c").setLanguage("c").setLines(20));

    assertEquals("java", CoverageRule.mostCommonLanguage(fs));

  }

  private static Language testLanguage(String name) {
    return new Language() {
      @Override
      public String getKey() {
        return name;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      public String[] getFileSuffixes() {
        return new String[0];
      }
    };
  }
}
