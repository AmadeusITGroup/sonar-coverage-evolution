# SonarQube Coverage Evolution plugin

[![Build Status](https://travis-ci.org/AmadeusITGroup/sonar-coverage-evolution.svg?branch=master)](https://travis-ci.org/AmadeusITGroup/sonar-coverage-evolution/branches)
[![SonarQube Quality Gate](https://sonarcloud.io/api/badges/gate?key=org.sonar:sonar-coverage-evolution-plugin&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)
[![Unit-Tests Overall Coverage](https://sonarcloud.io/api/badges/measure?key=org.sonar:sonar-coverage-evolution-plugin&metric=coverage&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)
[![Unit-Tests New Coverage](https://sonarcloud.io/api/badges/measure?key=org.sonar:sonar-coverage-evolution-plugin&metric=new_coverage&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)
[![SonarQube Reported Bugs](https://sonarcloud.io/api/badges/measure?key=org.sonar:sonar-coverage-evolution-plugin&metric=bugs&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)
[![SonarQube Reported Vulnerabilities](https://sonarcloud.io/api/badges/measure?key=org.sonar:sonar-coverage-evolution-plugin&metric=vulnerabilities&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)
[![Technical Debt](https://sonarcloud.io/api/badges/measure?key=org.sonar:sonar-coverage-evolution-plugin&metric=sqale_debt_ratio&template=FLAT)](https://sonarcloud.io/dashboard?id=org.sonar%3Asonar-coverage-evolution-plugin)


This SonarQube plugin creates issues if the coverage of a single file or the whole project has been reduced.

## Prerequisites
- Maven 3.0.5+
- JDK 1.8+
- SonarQube 5.x or 6.x (may work with 4.x)

## Usage

The plugin only works in SonarQube `preview` mode.
To enable the functionality enable the relevant rules in the used Quality Gate.
The rules belong to the `coverageEvolution-$LANGUAGE` rule repository.

## Caveats

* This plugin uses old, deprecated APIs and may stop working in future
  SonarQube versions.
* The plugin always compares the current coverage to the latest coverage
  reported to SonarQube, which may not be the actual target of the pullrequest.
