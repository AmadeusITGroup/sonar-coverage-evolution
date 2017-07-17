package org.sonar.plugins.coverageevolution;

import static org.sonar.plugins.coverageevolution.CoverageUtils.calculateCoverage;
import static org.sonar.plugins.coverageevolution.CoverageUtils.formatPercentage;
import static org.sonar.plugins.coverageevolution.CoverageUtils.roundedPercentageGreaterThan;

import java.text.MessageFormat;
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
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;

// We have to execute after all coverage sensors, otherwise we are not able to read their measurements
@Phase(name = Phase.Name.POST)
public class CoverageSensor implements Sensor, BatchComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageSensor.class);

  private final FileSystem fileSystem;
  private final ResourcePerspectives perspectives;
  private final CoverageConfiguration config;
  private ActiveRules activeRules;
  private CoverageProjectStore coverageProjectStore;


  public CoverageSensor(FileSystem fileSystem, ResourcePerspectives perspectives,
      CoverageConfiguration config, ActiveRules activeRules,
      CoverageProjectStore coverageProjectStore) {
    this.fileSystem = fileSystem;
    this.perspectives = perspectives;
    this.config = config;
    this.activeRules = activeRules;
    this.coverageProjectStore = coverageProjectStore;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    SonarClient sonar = new SonarClient(config.url(), config.login(), config.password());

    for (InputFile f : fileSystem.inputFiles(fileSystem.predicates().all())) {
      Integer linesToCover = null;
      Integer uncoveredLines = null;

      Resource fileResource = context.getResource(f);
      Measure<Integer> linesToCoverMeasure = context
          .getMeasure(fileResource, CoreMetrics.LINES_TO_COVER);
      if (linesToCoverMeasure != null) {
        linesToCover = linesToCoverMeasure.value();
      }

      Measure<Integer> uncoveredLinesMeasure = context
          .getMeasure(fileResource, CoreMetrics.UNCOVERED_LINES);
      if (uncoveredLinesMeasure != null) {
        uncoveredLines = uncoveredLinesMeasure.value();
      }

      // get lines_to_cover, uncovered_lines
      if ((linesToCover != null) && (uncoveredLines != null)) {
        Double previousCoverage = sonar.getMeasureValue(fileResource, CoreMetrics.LINE_COVERAGE);

        double coverage = calculateCoverage(linesToCover, uncoveredLines);

        coverageProjectStore.updateMeasurements(linesToCover, uncoveredLines);

        if (previousCoverage == null) {
          continue;
        }

        // The API returns the coverage rounded.
        // So we can only report anything if the rounded value has changed,
        // otherwise we could report false positives.
        LOGGER.debug("Previous/current file coverage: {} / {}", previousCoverage, coverage);
        if (roundedPercentageGreaterThan(previousCoverage, coverage)) {
          addIssue(f, coverage, previousCoverage);
        }
      }
    }

    // We assume the root module is always the last module, so that the overall data is correct
    if (module.isRoot()) {
      Double previousProjectCoverage = sonar.getMeasureValue(module, CoreMetrics.LINE_COVERAGE);
      Double projectCoverage = coverageProjectStore.getProjectCoverage();
      LOGGER.debug("Previous/current project-wide coverage: {} / {}", previousProjectCoverage,
          projectCoverage);
      if (roundedPercentageGreaterThan(previousProjectCoverage, projectCoverage)) {
        LOGGER.debug("Creating global coverage issue for {}", module);
        addIssue(module, projectCoverage, previousProjectCoverage);
      }
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
    addIssue(issuable,
        formatIssueMessage("the project", coverage, previousCoverage),
        CoverageRule.decreasingOverallLineCoverageRule(fileSystem));
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
        CoverageRule.decreasingLineCoverageRule(file));
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
    return CoverageUtils.shouldExecuteCoverage(config, activeRules);
  }
}
