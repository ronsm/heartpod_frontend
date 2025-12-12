#!/bin/bash
set -e

echo "🚀 Starting Android Emulator..."
echo "This will:"
echo "  1. Build the emulator Docker image (~2GB download)"
echo "  2. Start Android emulator"
echo "  3. Install and launch the Healthub app"
echo ""

# Check if APK exists
if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "❌ APK not found. Building it first..."
    docker compose run --rm build
fi

# Allow Docker to access X11
xhost +local:docker > /dev/null 2>&1

echo "⏳ Building emulator image (first time only, ~5 minutes)..."
docker compose build emulator

echo "🎬 Starting emulator..."
echo "   The emulator window will open shortly."
echo "   The app will auto-install and launch."
echo ""
echo "   Press Ctrl+C to stop the emulator."
echo ""

docker compose up emulator
