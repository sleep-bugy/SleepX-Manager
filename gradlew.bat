@echo off
setlocal

set DIR=%~dp0
set APP_HOME=%DIR%
for %%i in ("%APP_HOME%..") do set APP_HOME=%%~fi

set CLASSPATH="%APP_HOME%\gradle\wrapper\gradle-wrapper-main.jar;%APP_HOME%\gradle\wrapper\gradle-wrapper-shared.jar"

set JAVA_EXE=java
if defined JAVA_HOME set JAVA_EXE="%JAVA_HOME%\bin\java.exe"

%JAVA_EXE% -Dorg.gradle.appname=gradlew -classpath %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
