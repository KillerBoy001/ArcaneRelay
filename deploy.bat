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
)

copy ".\target\arcanerelay-%VERSION%.jar" "%HYTALE_MODS%\"