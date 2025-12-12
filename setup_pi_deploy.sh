#!/bin/bash
# Setup script to enable deploying from Pi 2
# Run this on your LAPTOP

PI_USER="openhabian"
PI_HOST="192.168.2.150"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
SCRIPT_PATH="install_on_temi.sh"

echo "📦 Step 1: Installing ADB on Pi 2..."
ssh -o StrictHostKeyChecking=no $PI_USER@$PI_HOST "sudo apt update && sudo apt install -y adb android-libadb"

echo "📤 Step 2: Creating directory on Pi 2..."
ssh -o StrictHostKeyChecking=no $PI_USER@$PI_HOST "mkdir -p ~/healthub-deploy"

echo "🚀 Step 3: Transferring APK and Script..."
scp -o StrictHostKeyChecking=no $APK_PATH $PI_USER@$PI_HOST:~/healthub-deploy/
scp -o StrictHostKeyChecking=no $SCRIPT_PATH $PI_USER@$PI_HOST:~/healthub-deploy/

echo "🔧 Step 4: Making script executable on Pi 2..."
ssh -o StrictHostKeyChecking=no $PI_USER@$PI_HOST "chmod +x ~/healthub-deploy/install_on_temi.sh"

echo "✅ Setup Complete!"
echo "Now you can SSH into the Pi and run:"
echo "  cd ~/healthub-deploy"
echo "  ./install_on_temi.sh"
