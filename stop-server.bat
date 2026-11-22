@echo off
title Stopping Plagiarism Server...

echo Finding process running on port 8080...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    echo Stopping process ID %%a...
    taskkill /F /PID %%a >nul 2>&1
)

echo [SUCCESS] Backend server stopped.
timeout /t 2 >nul
