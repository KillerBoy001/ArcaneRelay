@echo off

REM Load environment variables from .env file
if exist .env (
    for /f "delims=" %%i in (.env) do set %%i
)

REM Determine Maven profile (debug by default)
if not defined MAVEN_PROFILE (
    set MAVEN_PROFILE=debug
)

REM Allow override via command line argument
if not "%~1"=="" (
    set MAVEN_PROFILE=%~1
)

echo [INFO] Building with Maven profile: %MAVEN_PROFILE%

REM Build with the selected profile
call mvn clean install -P %MAVEN_PROFILE%
if %ERRORLEVEL% neq 0 (
    powershell Write-Host "[" -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host '] '-NoNewline; Write-Host "Maven build failed"
    exit /b 1
)

REM Extract version from pom.xml using PowerShell
for /f "delims=" %%i in ('powershell -Command "([xml](Get-Content .\pom.xml)).project.version"') do set VERSION=%%i

if exist "%HYTALE_SERVER_MODS_PATH%\arcanerelay-*.jar" (
    del /q "%HYTALE_SERVER_MODS_PATH%\arcanerelay-*.jar"
    powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host "Removed old mod from Hytale Mods folder"
)

copy ".\target\arcanerelay-%VERSION%.jar" "%HYTALE_SERVER_MODS_PATH%\" 
if %ERRORLEVEL% neq 0 (
    powershell Write-Host "[" -NoNewline; Write-Host "ERROR" -ForegroundColor Red -NoNewline; Write-Host '] '-NoNewline; Write-Host "Failed to copy mod to %HYTALE_SERVER_MODS_PATH%"
    exit /b 1
)
powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host "COPY SUCCES" -ForegroundColor Green


powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host 'DEPLOY COMPLETED' -ForegroundColor Yellow
powershell Write-Host ""
