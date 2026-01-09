#!/bin/bash
# Test HealthHub voice flow without Temi robot

echo "🤖 HealthHub Voice Flow Testing (No Robot Required)"
echo "=================================================="
echo ""

# 1. Start Android Emulator
echo "1️⃣ Starting Android Emulator..."
emulator -avd Pixel_3a_API_30 -no-snapshot-load &
sleep 10

# 2. Start mock Temi SDK (simulates robot responses)
echo "2️⃣ Starting Mock Temi Service..."
cd /home/kwalker96/Downloads/android-healthub-main
./gradlew :app:installDebug

# 3. Enable mock mode in app
echo "3️⃣ Enabling Mock Mode..."
adb shell am start -n org.hwu.care.healthub/.MainActivity \
  --es MOCK_MODE "true" \
  --es MOCK_VOICE_INPUT "yes I'm ready to start"

echo ""
echo "✅ Test Environment Ready!"
echo ""
echo "📱 App is running in MOCK MODE"
echo "   - Temi SDK calls are simulated"
echo "   - Voice input is mocked"
echo "   - AI responses are logged"
echo ""
echo "🎯 Test the flow:"
echo "   1. Tap 'Tap to Speak' button"
echo "   2. Mock voice input will be sent: 'yes I'm ready to start'"
echo "   3. Check logcat for AI response"
echo ""
echo "📊 Monitor logs:"
echo "   adb logcat | grep -E 'MainActivity|MockTemi|AsrResult'"
