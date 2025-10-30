#!/usr/bin/env sh

APP_HOME="$(cd "$(dirname "$0")"; pwd -P)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper-main.jar:$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar"

if [ -n "$JAVA_HOME" ] ; then
  JAVA_EXE="$JAVA_HOME/bin/java"
else
  JAVA_EXE="java"
fi

exec "$JAVA_EXE" -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
