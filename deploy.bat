@echo off

REM Load environment variables from .env file
if exist .env (
    for /f "delims=" %%i in (.env) do set %%i
)

call mvn clean install

REM Extract version from pom.xml using PowerShell
for /f "delims=" %%i in ('powershell -Command "([xml](Get-Content .\pom.xml)).project.version"') do set VERSION=%%i

if exist "%HYTALE_MODS%\arcanerelay-*.jar" (
    del /q "%HYTALE_MODS%\arcanerelay-*.jar"
    powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host "Removed old mod from Hytale Mods folder"
)

copy ".\target\arcanerelay-%VERSION%.jar" "%HYTALE_MODS%\" && powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host "COPY SUCCES" -ForegroundColor Green


powershell Write-Host "[" -NoNewline; Write-Host "INFO" -ForegroundColor Blue -NoNewline; Write-Host '] '-NoNewline; Write-Host 'DEPLOY COMPLETED' -ForegroundColor Yellow
powershell Write-Host ""
powershell Write-Host Press Enter to close window

pause>nul