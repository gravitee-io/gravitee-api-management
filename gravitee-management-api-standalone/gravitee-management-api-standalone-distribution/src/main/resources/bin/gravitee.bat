@echo off

set DIRNAME=%~dp0

rem set JAVA_HOME=C:\Java\jdk1.8.0_40

for %%B in (%~dp0\.) do set GRAVITEE_HOME=%%~dpB

IF "%JAVA_HOME%"=="" GOTO nojavahome

set JAVA_OPTS="-Djava.net.preferIPv4Stack=true"


set JAVA="%JAVA_HOME%/bin/java"

rem Setup the classpath
for /f %%i in ('dir ..\lib\gravitee-management-api-standalone-bootstrap-*.jar /s /b') do set runjar=%%i

set GRAVITEE_BOOT_CLASSPATH=%runjar%


# Display our environment
echo "========================================================================="
echo ""
echo "  Gravitee.IO Standalone Runtime Bootstrap Environment"
echo ""
echo "  GRAVITEE_HOME: %GRAVITEE_HOME%"
echo ""
echo "  JAVA: %JAVA%"
echo ""
echo "  JAVA_OPTS: %JAVA_OPTS%"
echo ""
echo "  CLASSPATH: %GRAVITEE_BOOT_CLASSPATH%  "
echo ""
echo "========================================================================="
echo ""

rem Execute the JVM in the foreground
%JAVA% %JAVA_OPTS% -cp %GRAVITEE_BOOT_CLASSPATH% -Dgravitee.home=%GRAVITEE_HOME% io.gravitee.management.standalone.boostrap.Bootstrap "%*"

set GRAVITEE_STATUS=%?
goto endbatch


:nojavahome
echo.
echo **************************************************
echo *
echo * WARNING ...
echo * JAVA_HOME must be set before starting Gravitee 
echo * Please check Java documentation to do it 
echo *
echo **************************************************
GOTO endbatch

:endbatch