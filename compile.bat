@echo off
set CLASSPATH=lib/DTNConsoleConnection.jar;lib/ECLA.jar;lib/junit-4.8.2.jar;lib/uncommons-maths-1.2.1.jar;.

javac -Xlint:deprecation -cp "%CLASSPATH%" core/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" movement/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" report/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" routing/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" gui/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" input/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" applications/*.java
javac -Xlint:deprecation -cp "%CLASSPATH%" interfaces/*.java
