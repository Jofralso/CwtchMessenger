@echo off
REM Cwtch Messenger - Windows Build Script
REM Creates distributable application for Windows

setlocal enabledelayedexpansion

set APP_NAME=CwtchMessenger
set APP_VERSION=1.0.0
set MAIN_CLASS=app.TerminalMain
set JAR_NAME=cwtch-terminal.jar

echo.
echo ╔═══════════════════════════════════════════════════════════════╗
echo ║           Cwtch Messenger - Windows Build                     ║
echo ╚═══════════════════════════════════════════════════════════════╝
echo.

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17+
    exit /b 1
)
echo [OK] Java detected

REM Build JAR
echo.
echo [1/4] Building JAR...
call mvn clean package -DskipTests -q
if not exist "target\%JAR_NAME%" (
    echo [ERROR] JAR build failed
    exit /b 1
)
echo [OK] JAR built: target\%JAR_NAME%

REM Create distribution structure
echo.
echo [2/4] Creating distribution structure...
if not exist "dist\bin" mkdir dist\bin
if not exist "dist\lib" mkdir dist\lib
if not exist "dist\docs" mkdir dist\docs

REM Copy JAR
copy target\%JAR_NAME% dist\lib\ >nul

REM Create Windows launcher
echo @echo off > dist\bin\cwtch.bat
echo set SCRIPT_DIR=%%~dp0 >> dist\bin\cwtch.bat
echo java -jar "%%SCRIPT_DIR%%..\lib\cwtch-terminal.jar" %%* >> dist\bin\cwtch.bat

REM Create PowerShell launcher
echo $ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path > dist\bin\cwtch.ps1
echo java -jar "$ScriptDir\..\lib\cwtch-terminal.jar" $args >> dist\bin\cwtch.ps1

echo [OK] Launcher scripts created

REM Create docs
echo.
echo [3/4] Creating documentation...
echo %APP_VERSION% > dist\VERSION
if exist README.md copy README.md dist\docs\ >nul
if exist LICENSE copy LICENSE dist\docs\ >nul

(
echo Cwtch Messenger v%APP_VERSION%
echo ==============================
echo.
echo INSTALLATION:
echo -------------
echo 1. Ensure Java 17+ is installed
echo 2. Extract this archive anywhere
echo 3. Run: bin\cwtch.bat
echo.
echo COMMAND LINE OPTIONS:
echo ---------------------
echo   --offline, -o    Start without Tor
echo   --no-color, -n   Disable ANSI colors
echo   --help, -h       Show help
echo.
echo QUICK START:
echo ------------
echo 1. Run: bin\cwtch.bat --offline
echo 2. Type: /help
echo 3. Type: /id
echo 4. Type: /quit
) > dist\docs\INSTALL.txt

echo [OK] Documentation created

REM Package
echo.
echo [4/4] Packaging...
set ARCHIVE_NAME=%APP_NAME%-%APP_VERSION%-windows
if exist "%ARCHIVE_NAME%.zip" del "%ARCHIVE_NAME%.zip"
powershell -command "Compress-Archive -Path 'dist\*' -DestinationPath '%ARCHIVE_NAME%.zip'" 2>nul
if exist "%ARCHIVE_NAME%.zip" (
    echo [OK] Created: %ARCHIVE_NAME%.zip
) else (
    echo [WARN] Could not create ZIP. Please compress 'dist' folder manually.
)

echo.
echo ════════════════════════════════════════════════════════════════
echo                     BUILD COMPLETE!
echo ════════════════════════════════════════════════════════════════
echo.
echo Run with: java -jar dist\lib\cwtch-terminal.jar --offline
echo Or:       dist\bin\cwtch.bat --offline
echo.

endlocal
