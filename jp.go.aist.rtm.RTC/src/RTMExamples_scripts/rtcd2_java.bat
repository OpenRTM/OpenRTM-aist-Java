@echo off

call set_classpath.bat
set CLASSPATH=%CLASSPATH%;%RTM_JAVA_ROOT%\jar\rtcd.jar;
java rtcd.rtcd  %*

