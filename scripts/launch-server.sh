#!/bin/bash
# Hytale Server Launch Script with Debug Support
# This script starts the Hytale server with JDWP debugging enabled

# Load environment variables from .env file
if [ -f .env ]; then
  export "$(cat .env)"
else
  echo "[ERROR] .env file not found. Please create it from .env.template"
  exit 1
fi

# Validate required environment variables
if [ -z "$HYTALE_SERVER_PATH" ]; then
  echo "[ERROR] HYTALE_SERVER_PATH not defined in .env"
  exit 1
fi

if [ -z "$HYTALE_SERVER_ASSETS_PATH" ]; then
  echo "[ERROR] HYTALE_SERVER_ASSETS_PATH not defined in .env"
  exit 1
fi

if [ -z "$HYTALE_DEBUG_PORT" ]; then
  echo "[WARNING] HYTALE_DEBUG_PORT not defined, defaulting to 5005"
  HYTALE_DEBUG_PORT=5005
fi

# Check if server jar exists
if [ ! -f "$HYTALE_SERVER_PATH/HytaleServer.jar" ]; then
  echo "[ERROR] HytaleServer.jar not found at $HYTALE_SERVER_PATH/HytaleServer.jar"
  exit 1
fi

# Check if assets exist
if [ ! -f "$HYTALE_SERVER_ASSETS_PATH" ]; then
  echo "[ERROR] Assets file not found at $HYTALE_SERVER_ASSETS_PATH"
  exit 1
fi

echo "[INFO] Starting Hytale Server with debugging..."
echo "[INFO] Debug Port: $HYTALE_DEBUG_PORT"
echo "[INFO] Server JAR: $HYTALE_SERVER_PATH/HytaleServer.jar"
echo "[INFO] Assets: $HYTALE_SERVER_ASSETS_PATH"
echo "[INFO] Server is waiting for debugger connection on port $HYTALE_DEBUG_PORT..."

# Give JDWP time to initialize
sleep 2

# Build and run the Java command with JDWP debugging
# The server will wait for a debugger to attach before starting
cd "$HYTALE_SERVER_PATH" || exit 1

java \
  -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=localhost:$HYTALE_DEBUG_PORT \
  -jar "$HYTALE_SERVER_PATH/HytaleServer.jar" \
  --assets "$HYTALE_SERVER_ASSETS_PATH" \
  --disable-sentry

# Check if the server exited with an error
if [ $? -ne 0 ]; then
  echo "[ERROR] Hytale Server exited with error code $?"
  exit 1
fi

echo "[INFO] Hytale Server stopped"
exit 0
