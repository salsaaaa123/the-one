@echo off
set CLASSPATH=.;lib/DTNConsoleConnection.jar;lib/ECLA.jar
java -Xmx512M -cp "%CLASSPATH%" core.DTNSim %*
