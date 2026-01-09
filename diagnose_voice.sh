#!/bin/bash
# Real-time voice diagnostics for HealthHub
# Monitors all voice-related events

echo "🎤 HealthHub Voice Diagnostics"
echo "=============================="
echo "Monitoring voice events in real-time..."
echo "Press Ctrl+C to stop"
echo ""

# Clear logcat
adb logcat -c

# Monitor voice-related logs
adb logcat | grep --line-buffered -E "MainActivity|AsrResult|AsrListener|SpeechRecognizer|handleSpeech|LangGraph|llmClient|TemiController|onAsrResult" | while read line; do
    timestamp=$(date +"%H:%M:%S")
    echo "[$timestamp] $line"
done
