@echo off
set CLASSPATH=lib\DTNConsoleConnection.jar;lib\ECLA.jar;lib\junit-4.8.2.jar;lib\uncommons-maths-1.2.1.jar;.
if not exist bin mkdir bin

for /r %%i in (*.java) do (
    javac -d bin -cp "%CLASSPATH%" %%i
)
