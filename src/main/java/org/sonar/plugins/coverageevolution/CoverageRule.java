package org.sonar.plugins.coverageevolution;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Languages;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RulesDefinition;

public class CoverageRule implements RulesDefinition {
  private final static Logger LOGGER = LoggerFactory.getLogger(CoverageRule.class);

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
      LOGGER.warn("The project does not contain any languages, skipping overall coverage issue");
      return Optional.empty();
    }

    Optional<String> language = mostCommonLanguage(fs);

    if (!language.isPresent()) {
      LOGGER.warn("Could not detect the language of the project, skipping overall coverage issue");
      return Optional.empty();
    }

    LOGGER.info("Using language \"{}\" for the project wide coverage", language.get());
    return Optional.of(RuleKey.of(
        repositoryName + "-" + language.get(), decreasingOverallLineCoverageRule));
  }

  static Optional<String> mostCommonLanguage(FileSystem fs) {
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
    );
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
