#!/bin/bash

# Build APK script with timestamp
# This script builds a release APK and saves it to dist/ with a timestamp

set -e  # Exit on error

# Get the project root directory (parent of bin/)
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

# Use default Java from macOS
if [ -x "/usr/libexec/java_home" ]; then
    export JAVA_HOME=$(/usr/libexec/java_home)
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Disable proxy settings (override global gradle.properties)
export GRADLE_OPTS="-Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort= -Dhttp.nonProxyHosts=* -Dhttps.nonProxyHosts=*"

echo "Building release APK..."
echo "Using Java: $(java -version 2>&1 | head -n 1)"

# Build the release APK
./gradlew assembleRelease -Dhttp.proxyHost= -Dhttp.proxyPort= -Dhttps.proxyHost= -Dhttps.proxyPort=

# Generate timestamp in format: YYYYMMDD-HHMMSS
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")

# Define source and destination paths
# Try both signed and unsigned APK names
APK_SOURCE=""
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    APK_SOURCE="app/build/outputs/apk/release/app-release.apk"
elif [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    APK_SOURCE="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    echo "Error: APK not found in app/build/outputs/apk/release/"
    echo "Available files:"
    ls -la app/build/outputs/apk/release/ 2>/dev/null || echo "Directory not found"
    exit 1
fi

APK_DEST="dist/wiotracker-${TIMESTAMP}.apk"

# Create dist directory if it doesn't exist
mkdir -p dist

# Copy APK to dist with timestamp
cp "$APK_SOURCE" "$APK_DEST"

echo "APK built successfully!"
echo "Source: $APK_SOURCE"
echo "Destination: $APK_DEST"
echo ""
echo "APK size: $(du -h "$APK_DEST" | cut -f1)"

# Copy APK to sync folder only if SYNC_FOLDER environment variable is set
if [ -n "$SYNC_FOLDER" ]; then
    SYNC_DEST="${SYNC_FOLDER}/apk/WIOTracker/wiotracker-${TIMESTAMP}.apk"
    
    # Create sync folder if it doesn't exist
    if [ ! -d "$SYNC_FOLDER" ]; then
        echo "Creating sync folder: $SYNC_FOLDER"
        mkdir -p "$SYNC_FOLDER"
    fi
    
    # Copy APK to sync folder
    echo ""
    echo "Copying APK to sync folder..."
    cp "$APK_SOURCE" "$SYNC_DEST"
    
    if [ $? -eq 0 ]; then
        echo "APK copied to sync folder successfully!"
        echo "Sync destination: $SYNC_DEST"
        echo "Sync folder size: $(du -h "$SYNC_DEST" | cut -f1)"
    else
        echo "Warning: Failed to copy APK to sync folder"
        exit 1
    fi
else
    echo ""
    echo "SYNC_FOLDER environment variable not set, skipping sync folder copy"
fi
