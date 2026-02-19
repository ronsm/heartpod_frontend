#!/bin/bash

# Temi IP Address
TEMI_IP="192.168.2.115"

# Find ADB
if command -v adb &> /dev/null; then
    ADB="adb"
elif [ -f "$HOME/Android/Sdk/platform-tools/adb" ]; then
    ADB="$HOME/Android/Sdk/platform-tools/adb"
elif [ -f "/usr/lib/android-sdk/platform-tools/adb" ]; then
    ADB="/usr/lib/android-sdk/platform-tools/adb"
else
    echo "Error: 'adb' command not found."
    exit 1
fi

echo "Resetting ADB connection..."
$ADB disconnect $TEMI_IP:5555
$ADB kill-server
sleep 2

echo "Checking network connection to $TEMI_IP..."
if ping -c 1 -W 2 $TEMI_IP &> /dev/null; then
    echo "Robot is reachable via WiFi."
else
    echo "Error: Cannot reach robot at $TEMI_IP"
    echo "Check: Is the robot connected to the SAME WiFi network as this computer?"
    echo "Check: Did the robot's IP address change? (Settings -> Network)"
    exit 1
fi

echo "Connecting to ADB..."
if $ADB connect $TEMI_IP:5555 | grep -q "connected"; then
    echo "ADB Connected."
else
    echo "Error: WiFi is working, but ADB refused connection."
    echo "Check: Go to Settings -> Developer Options -> Toggle 'USB Debugging' OFF and ON."
    exit 1
fi

# Check if device is actually online
if $ADB -s $TEMI_IP:5555 get-state | grep -q "device"; then
    echo "Device connected."
else
    echo "Error: Device is offline or unauthorized."
    echo "Check: Is the robot on? Is USB debugging enabled?"
    exit 1
fi

echo "Launching Healthub App..."
$ADB -s $TEMI_IP:5555 shell am start -n org.hwu.care.healthub/.MainActivity

echo "App launched."
