@echo off
cd /d "%~dp0"
title Plagiarism Detection System - LIVE LAUNCHER
echo =================================================================
echo   PLAGIARISM DETECTION SYSTEM (Java 17 + MongoDB + Public Link)
echo =================================================================
echo.

:: 1. Start Java Backend Server
echo [1/3] Starting Java Backend Server on port 8080...
start "Plagiarism Java Backend" /min java -cp "bin;lib/*" com.plagiarism.PlagiarismApplication 8080

timeout /t 3 >nul

:: 2. Open Local Browser
echo [2/3] Opening Local App in Browser...
start "" "http://localhost:8080"

:: 3. Start Public Cloudflare Tunnel
echo [3/3] Starting High-Speed Public Web Tunnel...
echo =================================================================
echo   LOOK BELOW FOR YOUR PUBLIC HTTPS URL (e.g. trycloudflare.com)
echo   Keep this window open to keep the public link active!
echo =================================================================
echo.
.\cloudflared.exe tunnel --url http://127.0.0.1:8080
pause
