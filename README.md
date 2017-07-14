# SonarQube Coverage Evolution plugin

This SonarQube plugin creates issues if the coverage of a single file or the whole project has been reduced.

## Prerequisites
- Maven 3.0.5+
- JDK 1.8+
- SonarQube 5.x (may work with 4.x, does not yet work with 6.x)

## Usage

The plugin only works in SonarQube `preview` mode.
To enable the functionality enable the relevant rules in the used Quality Gate.
The rules belong to the `coverageEvolution-$LANGUAGE` rule repository.
