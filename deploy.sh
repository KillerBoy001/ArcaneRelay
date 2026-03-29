#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
  export "$(cat .env)"
fi

mvn clean install

# Extract version from pom.xml
VERSION=$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -1)

rm -f "$HYTALE_MODS"/arcanerelay-*.jar
cp "./target/arcanerelay-$VERSION.jar" "$HYTALE_MODS"/