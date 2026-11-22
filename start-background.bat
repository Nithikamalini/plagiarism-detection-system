@echo off
title Starting Background Plagiarism Server...

:: Check if already running on port 8080
netstat -ano | findstr :8080 | findstr LISTENING >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] Plagiarism Server is ALREADY running on port 8080.
    echo Open your browser at http://localhost:8080
    timeout /t 3 >nul
    exit /b 0
)

echo Starting Plagiarism Server in background...
powershell -Command "Start-Process java -ArgumentList '-cp \"bin;lib/*\" com.plagiarism.PlagiarismApplication 8080' -WindowStyle Hidden"

timeout /t 2 >nul
echo [SUCCESS] Backend server started in background!
echo Web UI: http://localhost:8080
timeout /t 3 >nul
