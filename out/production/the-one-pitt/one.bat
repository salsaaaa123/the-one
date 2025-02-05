@echo off
set CLASSPATH=target;lib/ECLA.jar;lib/DTNConsoleConnection.jar
java -Xmx512M -cp "%CLASSPATH%" core.DTNSim %*
