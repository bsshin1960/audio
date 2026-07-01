# GravityMusic 100% Portable Auto-Build Script
# This script sets up a local JDK 17 environment temporarily to build the app without changing global JAVA_HOME.

$projectRoot = $PSScriptRoot
$jdkDir = Join-Path $projectRoot ".jdk17"
$jdkZip = Join-Path $projectRoot "openjdk17.zip"
# JDK 17 Download URL (Eclipse Temurin)
$jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.11%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.11_9.zip"
$expectedJdkHome = Join-Path $jdkDir "jdk-17.0.11+9"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting GravityMusic Auto Build & Setup System" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Download and Extract JDK 17 if not exists
if (-not (Test-Path $expectedJdkHome)) {
    Write-Host "[1/3] Java 17 not found. Downloading portable JDK 17..." -ForegroundColor Yellow
    Write-Host "Downloading... (approx. 180MB, please wait)" -ForegroundColor Gray
    
    # Disable progress bar for faster download speed
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip
    
    Write-Host "Extracting archive..." -ForegroundColor Gray
    if (-not (Test-Path $jdkDir)) {
        New-Item -ItemType Directory -Path $jdkDir | Out-Null
    }
    Expand-Archive -Path $jdkZip -DestinationPath $jdkDir -Force
    
    # Remove temporary zip file
    Remove-Item $jdkZip -Force
    Write-Host "-> Java 17 Setup Complete!" -ForegroundColor Green
} else {
    Write-Host "[1/3] Existing Java 17 environment detected." -ForegroundColor Green
}

# 2. Bind temporary Java Home
$env:JAVA_HOME = $expectedJdkHome
Write-Host "[2/3] Temporary JAVA_HOME set to: $env:JAVA_HOME" -ForegroundColor Green

# 3. Create Gradle Wrapper if missing
$gradlewPath = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    Write-Host "[3/3] Gradle Wrapper (gradlew) not found. Generating wrapper..." -ForegroundColor Yellow
    
    # Search for cached gradle.bat in user directory
    $cachedGradle = Get-ChildItem -Path "C:\Users\SBS\.gradle\wrapper\dists" -Filter "gradle.bat" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    
    if ($cachedGradle) {
        $gradleBatPath = $cachedGradle.FullName
        Write-Host "Found local cached Gradle: $gradleBatPath" -ForegroundColor Gray
        
        # Execute wrapper command under temporary JDK17
        & $gradleBatPath wrapper --gradle-version 8.7
    } else {
        Write-Error "Local Gradle cache not found. Please install Gradle or run with Android Studio once."
        exit 1
    }
} else {
    Write-Host "[3/3] Gradle Wrapper already exists." -ForegroundColor Green
}

# 4. Build and Install App on connected device
Write-Host "----------------------------------------------------------" -ForegroundColor Gray
Write-Host " Building and installing GravityMusic on your device..." -ForegroundColor Cyan
Write-Host " Make sure your smartphone is connected via USB with USB Debugging enabled." -ForegroundColor Yellow
Write-Host "----------------------------------------------------------" -ForegroundColor Gray

& .\gradlew installDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " Build & Installation SUCCESSFUL!" -ForegroundColor Green
    Write-Host " You can now start the DHU test following Plan & Help.md" -ForegroundColor Green
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "==========================================================" -ForegroundColor Red
    Write-Host " Build FAILED. Check error logs and device connection." -ForegroundColor Red
    Write-Host "==========================================================" -ForegroundColor Red
}
