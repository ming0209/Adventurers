#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
mkdir -p core/build/offline
find core/src/main/java core/src/validation/java -name '*.java' -print | sort > core/build/offline/sources.txt
"$javac_bin" -encoding UTF-8 -d core/build/offline @core/build/offline/sources.txt
"$java_bin" -cp core/build/offline dev.adventurers.core.SimulationTests core/build/test-results/simulation/TEST-simulation.xml
