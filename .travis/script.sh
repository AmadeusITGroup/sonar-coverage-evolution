#!/bin/sh

set -e

mvn -e mvn org.owasp:dependency-check-maven:check
