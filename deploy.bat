@echo off

REM Load environment variables from .env file
if exist .env (
    for /f "delims=" %%i in (.env) do set %%i
)

mvn clean install

REM Extract version from pom.xml using PowerShell
for /f "delims=" %%i in ('powershell -Command "(Select-Xml -Path pom.xml -XPath '//version').Node.InnerText"') do set VERSION=%%i

del /q "%HYTALE_MODS%\arcanerelay-*.jar"
copy "target\arcanerelay-%VERSION%.jar" "%HYTALE_MODS%\"