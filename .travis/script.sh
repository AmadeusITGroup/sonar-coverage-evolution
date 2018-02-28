#!/bin/sh

set -e

mvn -e org.owasp:dependency-check-maven:check
