@echo off
cd /d "%~dp0"
title Plagiarism Detection System - LIVE LAUNCHER
echo =================================================================
echo   PLAGIARISM DETECTION SYSTEM (Java 17 + MongoDB + Public Link)
echo =================================================================
echo.

:: Configure Java Environment
if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
    ) else if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: 1. Compile only if bin is missing
if not exist "bin\com\plagiarism\PlagiarismApplication.class" (
    echo [1/3] Compiling Java classes...
    if not exist "bin" mkdir "bin"
    javac --release 17 -encoding UTF-8 -cp "bin;lib/*" -d "bin" src\main\java\com\plagiarism\algo\*.java src\main\java\com\plagiarism\db\*.java src\main\java\com\plagiarism\model\*.java src\main\java\com\plagiarism\server\*.java src\main\java\com\plagiarism\util\*.java src\main\java\com\plagiarism\*.java
)

:: 2. Start Java Backend Server
echo [1/3] Starting Java Backend Server on port 8080...
start "Plagiarism Java Backend" java -cp "bin;lib/*" com.plagiarism.PlagiarismApplication 8080

timeout /t 2 >nul

:: 3. Open Local Browser
echo [2/3] Opening Local App in Browser...
start "" "http://localhost:8080"

:: 4. Start Public Cloudflare Tunnel
echo [3/3] Starting High-Speed Public Web Tunnel...
echo =================================================================
echo   COPY THE PUBLIC HTTPS URL BELOW (ending in .trycloudflare.com)
echo   Keep this window open to keep the public link active!
echo =================================================================
echo.
.\cloudflared.exe tunnel --url http://127.0.0.1:8080
pause
