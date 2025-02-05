@echo off
set CLASSPATH=lib/DTNConsoleConnection.jar;lib/ECLA.jar;lib/junit-4.8.2.jar;lib/uncommons-maths-1.2.1.jar;.
if not exist target mkdir target

javac -d target -cp "%CLASSPATH%" core/*.java
javac -d target -cp "%CLASSPATH%" movement/*.java
javac -d target -cp "%CLASSPATH%" report/*.java
javac -d target -cp "%CLASSPATH%" routing/*.java
javac -d target -cp "%CLASSPATH%" gui/*.java
javac -d target -cp "%CLASSPATH%" input/*.java
javac -d target -cp "%CLASSPATH%" applications/*.java
javac -d target -cp "%CLASSPATH%" interfaces/*.java
