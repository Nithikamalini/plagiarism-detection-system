@echo off
cd /d "%~dp0"
title Share Public URL (Cloudflare Tunnel)
echo =================================================================
echo   GENERATING INSTANT PUBLIC URL FOR YOUR PLAGIARISM SYSTEM
echo =================================================================
echo.
echo Make sure your server is running (run.bat or start-background.bat)
echo.
echo Starting secure high-speed Cloudflare public tunnel on port 8080...
.\cloudflared.exe tunnel --url http://127.0.0.1:8080
pause
