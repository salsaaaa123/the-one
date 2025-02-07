@echo off
set CLASSPATH=bin;lib/ECLA.jar;lib/DTNConsoleConnection.jar
java -Xmx512M -cp "%CLASSPATH%" core.DTNSim %*
