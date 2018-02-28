package org.sonar.plugins.coverageevolution;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issuable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.coverageevolution.client.SonarClient;

public class CoverageSensorTest {
  DefaultFileSystem fs;
  ResourcePerspectives rp = mock(ResourcePerspectives.class);
  Issuable issuable = mock(Issuable.class);
  Settings settings = new Settings();
  CoverageConfiguration config = new CoverageConfiguration(settings);
  CoverageProjectStore coverageProjectStore;
  SonarClient sonarClient;
  CoverageSensor sensor;
  Project project;

  @Before
  public void setUp() {
    project = new Project("foo:bar");
    settings.setProperty("sonar.scanAllFiles", true);

    when(rp.as(eq(Issuable.class), any(Resource.class))).thenReturn(issuable);
    when(rp.as(eq(Issuable.class), any(InputFile.class))).thenReturn(issuable);
    Issuable.IssueBuilder issueBuilder = mock(Issuable.IssueBuilder.class, Answers.RETURNS_SELF);
    when(issuable.newIssueBuilder()).thenReturn(issueBuilder);
  }

  private static ActiveRules makeRules(boolean line, boolean project, FileSystem fs) {
    ActiveRulesBuilder activeRulesBuilder = new ActiveRulesBuilder();
    List<NewActiveRule> rules = new ArrayList<>();
    if (line) {
      rules.add(activeRulesBuilder.create(CoverageRule.decreasingLineCoverageRule("java")));
    }
    if (project) {
      rules.add(activeRulesBuilder.create(CoverageRule.decreasingOverallLineCoverageRule(fs).get()));
    }
    return new DefaultActiveRules(rules);
  }

  @Test
  public void testShouldExecuteOnProjectWithPerFileRule() {
    sensor = new CoverageSensor(fs, rp, config, makeRules(true, false, null), coverageProjectStore, sonarClient);
    assertTrue(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testShouldExecuteOnProjectWithoutRules() {
    sensor = new CoverageSensor(fs, rp, config, makeRules(false, false, null), coverageProjectStore, sonarClient);
    assertFalse(sensor.shouldExecuteOnProject(project));
  }

  @Test
  public void testAnalyse() {
    File fsDir = new File("somepath");
    fs = new DefaultFileSystem(fsDir);

    InputFile fileA = new DefaultInputFile("a.java").setAbsolutePath("a.java").setLanguage("java");
    InputFile fileB = new DefaultInputFile("b.java").setAbsolutePath("b.java").setLanguage("java");

    fs.add(fileA);
    fs.add(fileB);
    fs.addLanguages("java");

    SensorContext context = mock(SensorContext.class);
    coverageProjectStore = new CoverageProjectStore();
    sonarClient = mock(SonarClient.class);

    sensor = new CoverageSensor(fs, rp, config, makeRules(true, true, fs), coverageProjectStore, sonarClient);

    mockPreviousCoverage(sonarClient, project, project, 100, 0);

    mockFileCoverages(context, sonarClient, fileA, project,
        100, 60,
        100, 50
    );

    mockFileCoverages(context, sonarClient, fileB, project,
        100, 70,
        100, 50
    );

    sensor.analyse(project, context);

    assertEquals(35.0, coverageProjectStore.getProjectCoverage(),0.01);

    verify(issuable, times(3)).addIssue(any());

    verify(sonarClient, times(3)).getMeasureValue(any(), any(), any());
    verifyNoMoreInteractions(sonarClient);
  }

  @Test
  public void testAnalyseWithExclusions() {

    File fsDir = new File("somepath");
    fs = new DefaultFileSystem(fsDir);

    InputFile fileA = new DefaultInputFile("a.java").setAbsolutePath("a.java").setLanguage("java");
    InputFile fileB = new DefaultInputFile("b.java").setAbsolutePath("b.java").setLanguage("java");
    fs.add(fileA);
    fs.add(fileB);
    fs.addLanguages("java");

    SensorContext context = mock(SensorContext.class);
    coverageProjectStore = new CoverageProjectStore();
    sonarClient = mock(SonarClient.class);

    mockFileCoverages(context, sonarClient, fileA, project,
        100, 60,
        100, 50
    );

    mockFileCoverages(context, sonarClient, fileB, project,
        100, 70,
        100, 50
    );

    sensor = new CoverageSensor(fs, rp, config, makeRules(true, false, null), coverageProjectStore, sonarClient);
    settings.setProperty(CoreProperties.PROJECT_COVERAGE_EXCLUSIONS_PROPERTY, "a.java");

    sensor.analyse(project, context);

    verify(sonarClient, times(2)).getMeasureValue(any(), any(), any());

  }

  private static void mockFileCoverages(
      SensorContext context, SonarClient client,
      InputFile file, Project module,
      int newLinesToCover, int newUncoveredLines,
      int oldLinesToCover, int oldUncoveredLines
  ) {
    Resource resource = new org.sonar.api.resources.File(file.relativePath());
    when(context.getResource(eq(file))).thenReturn(resource);

    mockPreviousCoverage(client, module, resource, oldLinesToCover, oldUncoveredLines);
    mockCurrentCoverage(context, resource, newLinesToCover, newUncoveredLines);
  }

  private static void mockPreviousCoverage(
      SonarClient client,
      Project module, Resource resource,
      int linesToCover, int uncoveredLines
  ) {
    double coverage = CoverageUtils.calculateCoverage(linesToCover, uncoveredLines);
    when(client.getMeasureValue(eq(module), eq(resource), eq(CoreMetrics.LINE_COVERAGE))).thenReturn(
        coverage
    );
  }

  private static void mockCurrentCoverage(
      SensorContext context, Resource resource,
      int linesToCover, int uncoveredLines
  ) {
    double coverage = CoverageUtils.calculateCoverage(linesToCover, uncoveredLines);
    when(context.getMeasure(eq(resource), eq(CoreMetrics.LINES_TO_COVER)))
        .thenReturn(new Measure<>(CoreMetrics.LINES_TO_COVER, (double) linesToCover));
    when(context.getMeasure(eq(resource), eq(CoreMetrics.UNCOVERED_LINES)))
        .thenReturn(new Measure<>(CoreMetrics.UNCOVERED_LINES, (double) uncoveredLines));
    when(context.getMeasure(eq(resource), eq(CoreMetrics.LINE_COVERAGE)))
        .thenReturn(new Measure<>(CoreMetrics.LINE_COVERAGE, coverage));
  }
}
