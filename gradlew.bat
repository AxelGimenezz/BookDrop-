@echo off
setlocal enabledelayedexpansion
set APP_HOME=%~dp0
set WRAPPER_DIR=%APP_HOME%gradle\wrapper
set WRAPPER_JAR=%WRAPPER_DIR%\gradle-wrapper.jar
set WRAPPER_URL=https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar
set WRAPPER_SHA256=498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
if not exist "%WRAPPER_JAR%" (
  echo Downloading Gradle wrapper...
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%.tmp'"
  if errorlevel 1 goto fail

  for /f "tokens=*" %%H in ('powershell -NoProfile -Command "(Get-FileHash -Algorithm SHA256 '%WRAPPER_JAR%.tmp').Hash.ToLower()"') do set ACTUAL_SHA=%%H
  if /I not "!ACTUAL_SHA!"=="%WRAPPER_SHA256%" (
    del /Q "%WRAPPER_JAR%.tmp" 2>NUL
    echo Invalid checksum for gradle-wrapper.jar.
    exit /b 1
  )

  move /Y "%WRAPPER_JAR%.tmp" "%WRAPPER_JAR%" >NUL
)

if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
) else (
  set JAVA_EXE=java.exe
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%

:fail
echo Could not download gradle-wrapper.jar. Check the Internet connection.
exit /b 1
