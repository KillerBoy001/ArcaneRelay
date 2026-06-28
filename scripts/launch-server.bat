@echo off
REM Hytale Server Launch Script with Debug Support
REM This script starts the Hytale server with JDWP debugging enabled

REM Load environment variables from .env file
if exist .env (
    for /f "delims=" %%i in (.env) do set %%i
) else (
    echo [ERROR] .env file not found. Please create it from .env.template
    exit /b 1
)

REM Validate required environment variables
if not defined HYTALE_SERVER_PATH (
    echo [ERROR] HYTALE_SERVER_PATH not defined in .env
    exit /b 1
)

if not defined HYTALE_SERVER_ASSETS_PATH (
    echo [ERROR] HYTALE_SERVER_ASSETS_PATH not defined in .env
    exit /b 1
)

if not defined HYTALE_DEBUG_PORT (
    echo [WARNING] HYTALE_DEBUG_PORT not defined, defaulting to 5005
    set HYTALE_DEBUG_PORT=5005
)

REM Check if server jar exists
if not exist "%HYTALE_SERVER_PATH%\HytaleServer.jar" (
    echo [ERROR] HytaleServer.jar not found at %HYTALE_SERVER_PATH%\HytaleServer.jar
    exit /b 1
)

REM Check if assets exist
if not exist "%HYTALE_SERVER_ASSETS_PATH%" (
    echo [ERROR] Assets file not found at %HYTALE_SERVER_ASSETS_PATH%
    exit /b 1
)

echo [INFO] Starting Hytale Server with debugging...
echo [INFO] Debug Port: %HYTALE_DEBUG_PORT%
echo [INFO] Server JAR: %HYTALE_SERVER_PATH%\HytaleServer.jar
echo [INFO] Assets: %HYTALE_SERVER_ASSETS_PATH%
echo [INFO] Server is waiting for debugger connection on port %HYTALE_DEBUG_PORT%...

REM Give JDWP time to initialize
timeout /t 2 /nobreak > nul

REM Build the Java command with JDWP debugging
REM The server will wait for a debugger to attach before starting
cd /d "%HYTALE_SERVER_PATH%"

java ^
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:%HYTALE_DEBUG_PORT% ^
  -jar "%HYTALE_SERVER_PATH%\HytaleServer.jar" ^
  --assets "%HYTALE_SERVER_ASSETS_PATH%" ^
  --disable-sentry

REM Check if the server exited with an error
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Hytale Server exited with error code %ERRORLEVEL%
    exit /b 1
)
