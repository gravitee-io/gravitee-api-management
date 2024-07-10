@echo off

set DIRNAME=%~dp0

rem set JAVA_HOME=C:\Java\jdk1.8.0_40

for %%B in (%~dp0\.) do set GRAVITEE_HOME=%%~dpB

IF "%JAVA_HOME%"=="" GOTO nojavahome

set JAVA_OPTS="-Djava.net.preferIPv4Stack=true"


set JAVA="%JAVA_HOME%/bin/java"

rem Setup the classpath
for /f %%i in ('dir ..\lib\gravitee-apim-rest-api-standalone-bootstrap-*.jar /s /b') do set runjar=%%i

set GRAVITEE_BOOT_CLASSPATH=%runjar%

if "%GIO_MIN_MEM%" == "" (
set GIO_MIN_MEM=256m
)

if "%GIO_MAX_MEM%" == "" (
set GIO_MAX_MEM=256m
)

REM min and max heap sizes should be set to the same value to avoid
REM stop-the-world GC pauses during resize
set JAVA_OPTS=%JAVA_OPTS% -Xms%GIO_MIN_MEM% -Xmx%GIO_MAX_MEM%

REM set to headless, just in case
set JAVA_OPTS=%JAVA_OPTS% -Djava.awt.headless=true

REM disable jersey WADL
set JAVA_OPTS=%JAVA_OPTS% -Djersey.config.allowSystemPropertiesProvider=true
set JAVA_OPTS=%JAVA_OPTS% -Djersey.config.server.wadl.disableWadl=true

REM Force the JVM to use IPv4 stack
if NOT "%ES_USE_IPV4%" == "" (
set JAVA_OPTS=%JAVA_OPTS% -Djava.net.preferIPv4Stack=true
)

REM Causes the JVM to dump its heap on OutOfMemory.
set JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError
REM The path to the heap dump location, note directory must exists and have enough
REM space for a full heap dump.
REM set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath=$GRAVITEE_HOME/logs/heapdump.hprof

REM Disables explicit GC
set JAVA_OPTS=%JAVA_OPTS% -XX:+DisableExplicitGC

REM Ensure UTF-8 encoding by default (e.g. filenames)
set JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8

REM Display our environment
echo "=============================================================="
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
echo "=============================================================="
echo ""

rem Execute the JVM in the foreground
%JAVA% %JAVA_OPTS% -cp %GRAVITEE_BOOT_CLASSPATH% -Dgravitee.home=%GRAVITEE_HOME% -Dvertx.disableFileCaching=true -Dvertx.disableFileCPResolving=true -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory io.gravitee.rest.api.standalone.boostrap.Bootstrap "%*"

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