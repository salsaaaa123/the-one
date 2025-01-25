@echo off

set CLASSPATH=lib/*;

echo Compiling core files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked core/*.java 
if %errorlevel% neq 0 goto :compilation_error

echo Compiling movement files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked movement/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling report files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked report/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling routing files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked routing/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling gui files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked gui/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling input files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked input/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling applications files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked applications/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compiling interfaces files...
javac -cp %CLASSPATH% -Xlint:-deprecation,unchecked interfaces/*.java
if %errorlevel% neq 0 goto :compilation_error

echo Compilation complete.
goto :end

:compilation_error
echo Compilation failed.
pause

:end