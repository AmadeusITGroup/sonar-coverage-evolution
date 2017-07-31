package org.sonar.plugins.coverageevolution;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
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

  public static Optional<RuleKey> decreasingOverallLineCoverageRule(FileSystem fs) {
    if (fs.languages().isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(RuleKey.of(
        repositoryName + "-" + mostCommonLanguage(fs), decreasingOverallLineCoverageRule));
  }

  static String mostCommonLanguage(FileSystem fs) {
    // we prefer a stable comparator
    Comparator<Map.Entry<String, Integer>> comparator = new ValueAndKeyComparator<>();

    return StreamSupport.stream(
        fs.inputFiles(fs.predicates().all()).spliterator(),
        false
    ).filter(f ->
        f != null && f.language() != null
    ).collect(Collectors.groupingBy(
        InputFile::language,
        Collectors.summingInt(InputFile::lines))
    ).entrySet().stream().filter(
        Objects::nonNull
    ).max(
        comparator
    ).map(
        Entry::getKey
    ).orElse(null);
  }

  private static class ValueAndKeyComparator<K extends Comparable, V extends Comparable> implements Comparator<Map.Entry<K, V>> {
    @Override
    public int compare(Entry<K, V> o1, Entry<K, V> o2) {
      int res = o1.getValue().compareTo(o2.getValue());
      if (res != 0) {
        return res;
      }
      return o1.getKey().compareTo(o2.getKey());
    }
  }

  public static RuleKey decreasingLineCoverageRule(String language) {
    return RuleKey.of(getRepositoryName(language), decreasingLineCoverageRule);
  }

  private static boolean isOurRepository(RuleKey rule) {
    return rule.repository().startsWith(repositoryName + "-");
  }

  public static boolean shouldExecute(ActiveRules rules) {
    return rules.findAll().stream().anyMatch(rule ->
        isOurRepository(rule.ruleKey())
    );
  }
}
