#!/bin/bash

CLASSPATH="lib/*:."

# Buat folder bin jika belum ada
mkdir -p bin

# Compile setiap package secara terpisah
javac --class-path "$CLASSPATH" -d bin core/*.java
javac --class-path "$CLASSPATH" -d bin movement/*.java
javac --class-path "$CLASSPATH" -d bin report/*.java
javac --class-path "$CLASSPATH" -d bin routing/*.java
javac --class-path "$CLASSPATH" -d bin gui/*.java
javac --class-path "$CLASSPATH" -d bin input/*.java
javac --class-path "$CLASSPATH" -d bin applications/*.java
javac --class-path "$CLASSPATH" -d bin interfaces/*.java
