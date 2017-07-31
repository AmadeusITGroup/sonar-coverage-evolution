package org.sonar.plugins.coverageevolution;

import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.coverageevolution.CoverageUtils.formatPercentage;
import static org.sonar.plugins.coverageevolution.CoverageUtils.roundedPercentageGreaterThan;

import java.text.MessageFormat;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.Phase;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.coverageevolution.client.SonarClient;

// We have to execute after all coverage sensors, otherwise we are not able to read their measurements
@Phase(name = Phase.Name.POST)
public class CoverageSensor implements Sensor, BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSensor.class);

  private final FileSystem fileSystem;
  private final ResourcePerspectives perspectives;
  private final CoverageConfiguration config;
  private ActiveRules activeRules;
  private CoverageProjectStore coverageProjectStore;
  private SonarClient sonar;


  public CoverageSensor(FileSystem fileSystem, ResourcePerspectives perspectives,
      CoverageConfiguration config, ActiveRules activeRules,
      CoverageProjectStore coverageProjectStore, SonarClient sonar) {
    this.fileSystem = fileSystem;
    this.perspectives = perspectives;
    this.config = config;
    this.activeRules = activeRules;
    this.coverageProjectStore = coverageProjectStore;
    this.sonar = sonar;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    for (InputFile file : fileSystem.inputFiles(fileSystem.predicates().doesNotMatchPathPatterns(config.coverageExclusions()))) {
      analyseFile(module, context, file);
    }

    // We assume the root module is always the last module, so that the overall data is correct
    if (module.isRoot()) {
      analyseRootProject(module);
    }
  }

  private Integer fetchMeasure(SensorContext context, Resource resource, Metric<Integer> metric) {
    Measure<Integer> measure = context.getMeasure(resource, metric);
    if (measure != null) {
      return measure.value();
    }
    LOGGER.warn("Could not retrieve measure of {} for {}", metric.getKey(), resource);
    return null;
  }

  private void analyseFile(Project module, SensorContext context, InputFile file) {
    Integer linesToCover = null;
    Integer uncoveredLines = null;

    Resource fileResource = context.getResource(file);
    linesToCover = fetchMeasure(context, fileResource, CoreMetrics.LINES_TO_COVER);
    uncoveredLines = fetchMeasure(context, fileResource, CoreMetrics.UNCOVERED_LINES);

    // get lines_to_cover, uncovered_lines
    if ((linesToCover != null) && (uncoveredLines != null)) {
      Double previousCoverage = sonar.getMeasureValue(module, fileResource, CoreMetrics.LINE_COVERAGE);

      double coverage = calculateCoverage(linesToCover, uncoveredLines);

      coverageProjectStore.updateMeasurements(linesToCover, uncoveredLines);

      if (previousCoverage == null) {
        return;
      }

      // The API returns the coverage rounded.
      // So we can only report anything if the rounded value has changed,
      // otherwise we could report false positives.
      LOGGER.debug("Previous/current file coverage on: {} / {}",
          fileResource.getPath(), previousCoverage, coverage);
      if (roundedPercentageGreaterThan(previousCoverage, coverage)) {
        addIssue(file, coverage, previousCoverage);
      }
    }
  }

  private void analyseRootProject(Project module) {
    Double previousProjectCoverage = sonar.getMeasureValue(module, module, CoreMetrics.LINE_COVERAGE);
    Double projectCoverage = coverageProjectStore.getProjectCoverage();
    LOGGER.debug("Previous/current project-wide coverage: {} / {}", previousProjectCoverage,
        projectCoverage);
    if (roundedPercentageGreaterThan(previousProjectCoverage, projectCoverage)) {
      LOGGER.debug("Creating global coverage issue for {}", module);
      addIssue(module, projectCoverage, previousProjectCoverage);
    }
  }

  private void addIssue(Issuable issuable, String message, RuleKey rule) {
    Issue issue = issuable.newIssueBuilder()
        .ruleKey(rule)
        .message(message)
        .build();
    issuable.addIssue(issue);
  }

  private void addIssue(Project project, double coverage, double previousCoverage) {
    Issuable issuable = perspectives.as(Issuable.class, (Resource) project);
    if (issuable == null) {
      LOGGER.warn("Could not get a perspective of Issuable to create an issue for {}, skipping",
          project);
      return;
    }

    Optional<RuleKey> ruleKey = CoverageRule.decreasingOverallLineCoverageRule(fileSystem);
    if (ruleKey.isPresent()) {
      addIssue(issuable,
          formatIssueMessage("the project", coverage, previousCoverage),
          ruleKey.get());
    } else {
      LOGGER.warn("Could not determine the RuleKey for the project {}", project.getEffectiveKey());
    }
  }

  private void addIssue(InputFile file, double coverage, double previousCoverage) {
    Issuable issuable = perspectives.as(Issuable.class, file);
    if (issuable == null) {
      LOGGER.warn("Could not get a perspective of Issuable to create an issue for {}, skipping",
          file);
      return;
    }
    addIssue(issuable,
        formatIssueMessage(file.relativePath(), coverage, previousCoverage),
        CoverageRule.decreasingLineCoverageRule(file.language()));
  }

  static String formatFileIssueMessage(String path, double coverage, double previousCoverage) {
    return formatIssueMessage("file " + path, coverage, previousCoverage);
  }

  private static String formatIssueMessage(String description, double coverage,
      double previousCoverage) {
    return MessageFormat.format("Line coverage of {0} lowered from {1}% to {2}%.",
        description, formatPercentage(previousCoverage), formatPercentage(coverage));
  }

  @Override
  public String toString() {
    return "Coverage Evolution Sensor";
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    // We only execute when run in preview mode
    // I don't know how we should behave during a normal scan

    if (!CoverageRule.shouldExecute(activeRules)) {
      return false;
    }

    if (!config.scanAllFiles()) {
      LOGGER.warn(
          "Not scanning all files, coverage features will be unreliable and will be disabled");
      return false;
    }

    return true;
  }
}
