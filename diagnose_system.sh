#!/bin/bash
# Complete system diagnostics for HealthHub

echo "🔍 HealthHub System Diagnostics"
echo "================================"
echo ""

# 1. Check if app is running
echo "1. App Status:"
if adb shell pidof org.hwu.care.healthub > /dev/null; then
    PID=$(adb shell pidof org.hwu.care.healthub)
    echo "   ✅ App is running (PID: $PID)"
else
    echo "   ❌ App is NOT running"
fi
echo ""

# 2. Check Temi SDK version
echo "2. Temi SDK Info:"
adb shell dumpsys package org.hwu.care.healthub | grep -A 5 "versionName" | head -6
echo ""

# 3. Check microphone permissions
echo "3. Microphone Permission:"
PERM=$(adb shell dumpsys package org.hwu.care.healthub | grep "android.permission.RECORD_AUDIO" -A 1 | grep "granted=")
echo "   $PERM"
echo ""

# 4. Check recent crashes
echo "4. Recent Crashes:"
adb logcat -d | grep -E "FATAL|AndroidRuntime" | grep "healthub" | tail -5
if [ $? -ne 0 ]; then
    echo "   ✅ No recent crashes"
fi
echo ""

# 5. Check Temi robot status
echo "5. Temi Robot Status:"
adb logcat -d | grep -E "Robot.*ready|AsrListener.*register" | tail -3
echo ""

# 6. Check voice events (last 30 seconds)
echo "6. Recent Voice Events:"
adb logcat -d -t '30.0' | grep -E "AsrResult|onAsrResult|handleSpeech" | tail -10
if [ $? -ne 0 ]; then
    echo "   ⚠️  No voice events detected"
fi
echo ""

# 7. Check AI agent connectivity
echo "7. AI Agent Status:"
curl -s http://192.168.2.150:8000/health > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✅ LangGraph agent reachable"
else
    echo "   ❌ LangGraph agent NOT reachable"
fi
echo ""

# 8. Check OpenHAB connectivity
echo "8. OpenHAB Status:"
curl -s http://192.168.2.150:8080/rest/items > /dev/null
if [ $? -eq 0 ]; then
    echo "   ✅ OpenHAB reachable"
else
    echo "   ❌ OpenHAB NOT reachable"
fi
echo ""

echo "================================"
echo "Diagnostics complete!"
echo ""
echo "To monitor voice in real-time, run:"
echo "  ./diagnose_voice.sh"
