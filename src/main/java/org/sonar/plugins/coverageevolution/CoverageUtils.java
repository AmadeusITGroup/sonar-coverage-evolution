package org.sonar.plugins.coverageevolution;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public final class CoverageUtils {
 
  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageUtils.class); 
	
  public static final int MAX_PERCENTAGE = 100;

  private CoverageUtils() {}

  public static double calculateCoverage(int linesToCover, int uncoveredLines) {
    if (linesToCover == 0) {
      return MAX_PERCENTAGE;
    }

    return (1 - (((double) uncoveredLines) / linesToCover)) * MAX_PERCENTAGE;
  }

  public static String formatPercentage(double d) {

    // Defining that our percentage is precise down to 0.1%
    DecimalFormat df = new DecimalFormat("0.0");

    // Protecting this method against non-US locales that would not use '.' as decimal separation
    DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
    decimalFormatSymbols.setDecimalSeparator('.');
    df.setDecimalFormatSymbols(decimalFormatSymbols);

    // Making sure that we round the 0.1% properly out of the double value
    df.setRoundingMode(RoundingMode.HALF_UP);
    return df.format(d);
  }

  public static boolean roundedPercentageGreaterThan(double left, double right) {
    return (left > right) && !formatPercentage(left).equals(formatPercentage(right));
  }

  // SQ 6.x does not provide the effectiveKey anymore directly for files
  // It is still provided for modules, which makes the following code work for files and modules
  // for both SQ 5.x and 6.x
  public static String computeEffectiveKey(Resource resource, Project module) {
    LOGGER.debug("Resource effectiveKey = {}, resource key = {}, module key = {}, module branch = {}",
        resource.getEffectiveKey(), resource.getKey(), module.getKey(), module.getBranch());
	  
  
	String effectiveKey = resource.getEffectiveKey();
	if (effectiveKey == null) {
		effectiveKey = module.getKey() + ":"; 
        if (module.getBranch() != null && !"".equals(module.getBranch())) {
        	effectiveKey += module.getBranch() + ":";
        }
        effectiveKey += resource.getKey(); 
	}
	
	LOGGER.debug("Resulted effective key = {}", effectiveKey);
    return effectiveKey;
  }
}
