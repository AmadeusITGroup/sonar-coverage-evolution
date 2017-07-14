package org.sonar.plugins.coverageevolution;

import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class CoverageRule implements RulesDefinition {

  private Languages languages;
  private static String repositoryName = "coverageEvolution";
  private static String decreasingLineCoverageRule = "decreasingLineCoverage";
  private static String decreasingOverallLineCoverageRule = "decreasingOverallLineCoverage";

  public CoverageRule(Languages languages) {
    this.languages = languages;
  }

  private static String getRepositoryName(String language) {
    return repositoryName + "-" + language;
  }

  @Override
  public void define(Context context) {
    for (ExtendedRepository repo : context.repositories()) {
      System.out.println("Repo: " + repo.key() + " " + repo.language());
    }
    for (Language language : languages.all()) {
      NewRepository repo = context
          .createRepository(getRepositoryName(language.getKey()), language.getKey());
      repo.setName("Coverage evolution");

      repo.createRule(decreasingLineCoverageRule)
          .setName("Line-coverage on files should not decrease")
          .setMarkdownDescription("Reports if the line-coverage on a file has decreased.")
          .setTags("bad-practice")
          .setSeverity(Severity.BLOCKER)
      ;
      repo.createRule(decreasingOverallLineCoverageRule)
          .setName("Project-wide line-coverage should not decrease")
          .setMarkdownDescription("Reports if the line-coverage on the project has decreased.")
          .setTags("bad-practice")
          .setSeverity(Severity.BLOCKER)
      ;

      repo.done();
    }
  }

  public static RuleKey decreasingOverallLineCoverageRule(FileSystem fs) {
    return RuleKey
        .of(repositoryName + "-" + fs.languages().first(), decreasingOverallLineCoverageRule);
  }

  public static RuleKey decreasingLineCoverageRule(InputFile file) {
    return decreasingLineCoverageRule(file.language());
  }

  public static RuleKey decreasingLineCoverageRule(String language) {
    return RuleKey.of(getRepositoryName(language), decreasingLineCoverageRule);
  }

  public static boolean isDecreasingLineCoverage(Issue issue) {
    return isDecreasingLineCoverage(issue.ruleKey());
  }

  public static boolean isDecreasingLineCoverage(RuleKey rule) {
    return isOurRepository(rule) && decreasingLineCoverageRule.equals(rule.rule());
  }

  public static boolean isOurRepository(RuleKey rule) {
    return rule.repository().startsWith(repositoryName + "-");
  }

  public static boolean shouldExecute(ActiveRules rules) {
    return rules.findAll().stream().anyMatch((rule) ->
        isOurRepository(rule.ruleKey())
    );
  }
}
