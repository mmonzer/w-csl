@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  w-csl startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and W_CSL_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\w-csl.jar;%APP_HOME%\lib\xminiserver.jar;%APP_HOME%\lib\lombok-1.18.4.jar;%APP_HOME%\lib\junit-platform-engine-1.11.0-M2.jar;%APP_HOME%\lib\junit-platform-commons-1.11.0-M2.jar;%APP_HOME%\lib\junit-jupiter-engine-5.11.0-M2.jar;%APP_HOME%\lib\junit-jupiter-api-5.11.0-M2.jar;%APP_HOME%\lib\json-20210307.jar;%APP_HOME%\lib\fluent-hc-4.5.13.jar;%APP_HOME%\lib\javalin-2.4.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.2.71.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.2.71.jar;%APP_HOME%\lib\kotlin-stdlib-1.2.71.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\bson-4.2.0.jar;%APP_HOME%\lib\org-apache-commons-codec-RELEASE190.jar;%APP_HOME%\lib\org-apache-commons-io-RELEASE113.jar;%APP_HOME%\lib\nrjavaserial-3.12.1.jar;%APP_HOME%\lib\commons-lang3-3.8.1.jar;%APP_HOME%\lib\org-apache-commons-logging-RELEASE210.jar;%APP_HOME%\lib\lightcouch-0.2.0.jar;%APP_HOME%\lib\gson-2.8.6.jar;%APP_HOME%\lib\com-google-guava-RELEASE113.jar;%APP_HOME%\lib\httpclient-4.5.13.jar;%APP_HOME%\lib\org.apache.httpcomponents.httpcore-4.1.2.jar;%APP_HOME%\lib\jjwt-0.9.1.jar;%APP_HOME%\lib\jackson-databind-2.11.2.jar;%APP_HOME%\lib\jackson-annotations-2.11.2.jar;%APP_HOME%\lib\jackson-core-2.11.2.jar;%APP_HOME%\lib\java-json-1.2.1.jar;%APP_HOME%\lib\jbcrypt-0.4.jar;%APP_HOME%\lib\jetty-quickstart-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-jmx-9.4.14.v20181114.jar;%APP_HOME%\lib\javax-websocket-server-impl-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-annotations-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-plus-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-jndi-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-servlets-9.4.14.v20181114.jar;%APP_HOME%\lib\spark-core-2.8.0.jar;%APP_HOME%\lib\websocket-server-9.4.14.v20181114.jar;%APP_HOME%\lib\javax-websocket-client-impl-9.4.14.v20181114.jar;%APP_HOME%\lib\websocket-client-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-client-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-rewrite-9.4.14.v20181114.jar;%APP_HOME%\lib\http2-server-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-jaspi-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-deploy-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-webapp-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-servlet-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-security-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-server-9.4.14.v20181114.jar;%APP_HOME%\lib\http2-client-9.4.14.v20181114.jar;%APP_HOME%\lib\http2-common-9.4.14.v20181114.jar;%APP_HOME%\lib\http2-hpack-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-http-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-alpn-client-9.4.14.v20181114.jar;%APP_HOME%\lib\websocket-common-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-io-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-xml-9.4.14.v20181114.jar;%APP_HOME%\lib\jetty-util-9.4.28.v20200408.jar;%APP_HOME%\lib\jna-3.0.9.jar;%APP_HOME%\lib\jsch-0.1.7.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\micrometer-observation-1.10.4.jar;%APP_HOME%\lib\micrometer-commons-1.10.4.jar;%APP_HOME%\lib\mongo-java-driver-3.12.8.jar;%APP_HOME%\lib\org.eclipse.paho.client.mqttv3-1.2.5.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\spring-websocket-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-context-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-aop-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-messaging-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-web-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-beans-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-expression-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-core-5.2.22.RELEASE.jar;%APP_HOME%\lib\spring-jcl-5.2.22.RELEASE.jar;%APP_HOME%\lib\org.apache.servicemix.bundles.velocity-1.7_6.jar;%APP_HOME%\lib\opentest4j-1.3.0.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\httpcore-4.4.13.jar;%APP_HOME%\lib\websocket-servlet-9.4.14.v20181114.jar;%APP_HOME%\lib\javax.websocket-api-1.0.jar;%APP_HOME%\lib\javax.servlet-api-3.1.0.jar;%APP_HOME%\lib\javax.mail.glassfish-1.4.1.v201005082020.jar;%APP_HOME%\lib\commons-net-3.3.jar;%APP_HOME%\lib\commons-collections-3.2.1.jar;%APP_HOME%\lib\commons-lang-2.4.jar;%APP_HOME%\lib\commons-codec-1.11.jar;%APP_HOME%\lib\javax.annotation-api-1.2.jar;%APP_HOME%\lib\asm-commons-7.0.jar;%APP_HOME%\lib\asm-analysis-7.0.jar;%APP_HOME%\lib\asm-tree-7.0.jar;%APP_HOME%\lib\asm-7.0.jar;%APP_HOME%\lib\javax.security.auth.message-1.0.0.v201108011116.jar;%APP_HOME%\lib\jetty-continuation-9.4.14.v20181114.jar;%APP_HOME%\lib\javax.transaction-api-1.3.jar;%APP_HOME%\lib\websocket-api-9.4.14.v20181114.jar;%APP_HOME%\lib\javax.activation-1.1.0.v201105071233.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.2.71.jar;%APP_HOME%\lib\javax.websocket-client-api-1.0.jar


@rem Execute w-csl
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %W_CSL_OPTS%  -classpath "%CLASSPATH%" main.CSLIDSMainClient %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable W_CSL_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%W_CSL_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
