@echo off
REM ============================================================
REM start-backend.bat
REM One-click script to start the Spring Boot backend
REM
REM WHAT THIS DOES:
REM   1. Sets JAVA_HOME to your Java 17 installation
REM   2. Runs the Spring Boot application using IntelliJ's bundled Maven
REM
REM HOW TO USE:
REM   Double-click this file, OR run it from VS Code terminal: .\start-backend.bat
REM   Wait for: "Started BookstoreApplication on port 8080"
REM ============================================================

SET JAVA_HOME=C:\Program Files\Java\jdk-17
SET MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\plugins\maven-plugin\lib\maven3\bin\mvn.cmd

echo.
echo ======================================================
echo  Starting Bookstore Backend...
echo  Database : localhost:5432/bookstore
echo  API URL  : http://127.0.0.1:8080
echo ======================================================
echo.

"%MVN%" spring-boot:run -f backend/pom.xml

pause
