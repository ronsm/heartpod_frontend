#!/bin/bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
TEMI_IP="192.168.2.115"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "🔄 Resetting ADB..."
$ADB disconnect
$ADB kill-server
sleep 2

echo "🚀 Connecting to Temi at $TEMI_IP..."
$ADB connect $TEMI_IP:5555

echo "🗑️ Uninstalling old version..."
$ADB -s $TEMI_IP:5555 uninstall org.hwu.care.healthub

echo "📦 Installing APK..."
$ADB -s $TEMI_IP:5555 install -r -t $APK_PATH

echo "📱 Launching App..."
$ADB -s $TEMI_IP:5555 shell am start -n org.hwu.care.healthub/.MainActivity

echo "✅ Deployment script finished."
