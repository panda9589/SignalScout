#!/usr/bin/env bash

set -e

# Create gradle wrapper
cd backend

# If gradle wrapper doesn't exist, create it
if [ ! -f "gradlew" ]; then
    gradle wrapper --gradle-version 8.3
fi

chmod +x gradlew
cd ..
