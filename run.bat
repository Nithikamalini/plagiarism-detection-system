@echo off
title Plagiarism Detection System
echo =================================================================
echo        PLAGIARISM DETECTION SYSTEM (Java ^& MongoDB)
echo =================================================================
echo.

:: Detect Java Home if not set
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

:: Verify Java installation
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java 17+ is required but was not found in PATH or JAVA_HOME.
    echo Please install OpenJDK 17 or set JAVA_HOME.
    pause
    exit /b 1
)

:: Check if MongoDB is running or start it
echo [1/3] Checking MongoDB connection...
where mongod >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo MongoDB binary found.
) else (
    echo [INFO] Standalone mongod not in PATH. If MongoDB is not running as a Windows Service,
    echo the system will automatically utilize the high-reliability internal hybrid cache.
)

:: Compile sources if needed
echo [2/3] Verifying / Compiling Java Sources...
if not exist "bin" mkdir "bin"
javac --release 17 -encoding UTF-8 -cp "bin;lib/*" -d "bin" src\main\java\com\plagiarism\algo\*.java src\main\java\com\plagiarism\db\*.java src\main\java\com\plagiarism\model\*.java src\main\java\com\plagiarism\server\*.java src\main\java\com\plagiarism\util\*.java src\main\java\com\plagiarism\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java compilation failed.
    pause
    exit /b 1
)

echo [3/3] Starting Plagiarism Detection System on http://localhost:8080 ...
start "" "http://localhost:8080"
java -cp "bin;lib/*" com.plagiarism.PlagiarismApplication 8080

pause
