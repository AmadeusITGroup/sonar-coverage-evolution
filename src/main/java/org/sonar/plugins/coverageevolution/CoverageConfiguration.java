package org.sonar.plugins.coverageevolution;

import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.config.Settings;

@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class CoverageConfiguration implements BatchComponent {
  private Settings settings;

  public CoverageConfiguration(Settings settings) {
    this.settings = settings;
  }

  public String url() {
    return settings.getString("sonar.host.url");
  }

  public String login() {
    return settings.getString(CoreProperties.LOGIN);
  }

  public String password() {
    return settings.getString(CoreProperties.PASSWORD);
  }

  public boolean scanAllFiles() {
    return settings.getBoolean("sonar.scanAllFiles");
  }
}
