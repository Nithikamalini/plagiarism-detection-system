# Plagiarism Detection System - PowerShell Runner
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "        PLAGIARISM DETECTION SYSTEM (Java & MongoDB)" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""

# Ensure JAVA_HOME
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot") {
        $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
    } elseif (Test-Path "C:\Program Files\Java\jdk-17") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    }
}

if ($env:JAVA_HOME) {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}

# Compile
Write-Host "[1/3] Compiling Java classes..." -ForegroundColor Yellow
if (-not (Test-Path ".\bin")) { New-Item -ItemType Directory -Path ".\bin" | Out-Null }
$sources = Get-ChildItem -Path ".\src\main\java" -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac --release 17 -encoding UTF-8 -cp "bin;lib/*" -d ".\bin" $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit 1
}

# Launch Browser
Write-Host "[2/3] Opening browser at http://localhost:8080..." -ForegroundColor Yellow
Start-Process "http://localhost:8080"

# Start Application
Write-Host "[3/3] Starting Java server..." -ForegroundColor Green
java -cp "bin;lib/*" com.plagiarism.PlagiarismApplication 8080
