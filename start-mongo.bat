@echo off
title MongoDB Server 27017
echo Starting MongoDB local daemon on port 27017...

set "MONGO_BIN=C:\Program Files\MongoDB\Server\8.0\bin\mongod.exe"
if not exist "%MONGO_BIN%" (
    set "MONGO_BIN=C:\Program Files\MongoDB\Server\8.2\bin\mongod.exe"
)

if not exist "%MONGO_BIN%" (
    echo [ERROR] Could not find mongod.exe in Program Files\MongoDB.
    echo Please start MongoDB using Windows Services or ensure it is in your PATH.
    pause
    exit /b 1
)

if not exist "data\db" mkdir "data\db"
"%MONGO_BIN%" --dbpath "data\db" --bind_ip 127.0.0.1 --port 27017
pause
