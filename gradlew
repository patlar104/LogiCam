#!/bin/sh
# Gradle wrapper script
GRADLE_APP_NAME=Gradle
APP_BASE_NAME=${0##*/}
APP_HOME=$( cd "${0%/*}/.." && pwd )
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -cp "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
