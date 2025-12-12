#!/bin/bash

# Temi IP Address (from user)
TEMI_IP="192.168.2.115"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Find ADB
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "$HOME/Android/Sdk/platform-tools/adb" ]; then
    ADB="$HOME/Android/Sdk/platform-tools/adb"
elif [ -f "/usr/lib/android-sdk/platform-tools/adb" ]; then
    ADB="/usr/lib/android-sdk/platform-tools/adb"
else
    echo "❌ Error: 'adb' command not found."
    echo "Please install it with: sudo apt install adb"
    echo "OR add Android SDK platform-tools to your PATH."
    exit 1
fi

echo "🚀 Connecting to Temi at $TEMI_IP..."
$ADB connect $TEMI_IP:5555

echo "🗑️ Uninstalling old version (if any)..."
$ADB uninstall org.hwu.care.healthub

echo "📦 Installing Healthub App..."
$ADB install -r $APK_PATH

echo "📱 Launching App..."
$ADB shell am start -n org.hwu.care.healthub/.MainActivity

echo "✅ Done! The app should be open now."
echo "ℹ️ Note: On Temi, if it closes, find it in 'Apps' list (top-right menu)."
