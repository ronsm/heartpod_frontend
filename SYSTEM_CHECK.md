# HealthHub System Verification Guide

Use this checklist to verify all components are running correctly on the Temi robot and Raspberry Pi.

---

## 🔍 Part 1: Raspberry Pi Health Check

### Step 1: Verify All Services Are Running
```bash
ssh openhabian@192.168.2.150
systemctl status openhab mosquitto healthub-ble healthub-agent --no-pager
```

**Expected Output:**
- ✅ All 4 services show `active (running)`
- ✅ No red "failed" text

---

### Step 2: Check AI Agent Health Endpoint
```bash
curl http://192.168.2.150:8000/health
```

**Expected Output:**
```json
{"status":"healthy"}
```

---

### Step 3: Verify BLE Monitor is Reading Sensors
```bash
sudo journalctl -u healthub-ble -n 50 --no-pager
```

**Expected Output:**
- ✅ Lines showing "Connected to device" or "Reading data"
- ✅ No repeated connection errors
- ⚠️ If you see "Device not found", the Pulse Oximeter is off or out of range

---

### Step 4: Check OpenHAB Items
```bash
curl -H "Authorization: Bearer oh.NHSTHT.QViW3MMVzsp56R8PNt3maoKrv9Z7iP7LNRymiPG25bYqlOXgV0BgggwQ8ZCbBbBdPTy6WxbBW0u0BBqCkiG9w" \
  http://192.168.2.150:8080/rest/items/PulseOximeter_SpO2
```

**Expected Output:**
```json
{
  "state": "98",
  "type": "Number",
  ...
}
```

---

### Step 5: Test MQTT Broker
```bash
# On your PC (not the Pi):
mosquitto_pub -h 192.168.2.150 -t test/topic -m "hello"
```

**Expected Output:**
- ✅ No errors
- ⚠️ If "Connection refused", check `systemctl status mosquitto`

---

## 📱 Part 2: Temi Robot App Check

### Step 6: Physical Access to Temi
1. **Wake up the Temi** (tap the screen)
2. **Find the HealthHub app icon** on the home screen
3. **Launch the app**

---

### Step 7: Check Network Connectivity (In App)
Look at the app's UI:
- ✅ **Top status bar**: Should show "Connected" or a green indicator
- ❌ **If "Disconnected"**: The app cannot reach the Pi

**Troubleshooting:**
```bash
# On Temi (via ADB):
adb connect 192.168.2.115
adb shell ping -c 3 192.168.2.150
```

---

### Step 8: Test Voice Interaction
1. **Tap the microphone button** (or say "Hey Temi")
2. **Say:** "What is my health status?"
3. **Expected:** Temi responds with current readings or "I don't have recent data"

**If no response:**
- Check Logcat for errors:
  ```bash
  adb logcat | grep -i "healthub\|langgraph"
  ```

---

### Step 9: Test Manual Reading
1. **Put your finger in the Pulse Oximeter** (turn it on)
2. **In the app:** Tap "Start Check" or "Measure"
3. **Expected:** 
   - App shows "Scanning for device..."
   - After 5-10 seconds: SpO2 and Heart Rate appear

**If stuck on "Scanning":**
- BLE service might not be running (check Step 3)

---

### Step 10: Verify AI Agent Response
1. **In the app:** Type or say "I need help"
2. **Expected:** Temi says something like:
   > "I can help you check your blood pressure or oxygen levels."

**If you get an error:**
- Check if `GOOGLE_API_KEY` is set (Step 11)

---

## 🔑 Part 3: API Key Verification

### Step 11: Confirm Gemini Key is Set
```bash
ssh openhabian@192.168.2.150
cat ~/langraph-backend/.env
```

**Expected Output:**
```
GOOGLE_API_KEY=AIza...your-actual-key
OPENHAB_URL=http://192.168.2.150:8080
OPENHAB_API_TOKEN=oh.NHSTHT...
```

**If you see `sk-placeholder`:**
- You forgot to add your Gemini key!
- Get one at: https://aistudio.google.com/apikey
- Edit: `nano ~/langraph-backend/.env`
- Restart: `sudo systemctl restart healthub-agent`

---

## 🚨 Quick Troubleshooting

| Problem | Solution |
|:--------|:---------|
| **Pi not responding** | `ping 192.168.2.150` - If fails, check Wi-Fi |
| **Agent returns errors** | Check logs: `journalctl -u healthub-agent -n 50` |
| **App can't connect** | Verify IP in `MainActivity.kt` is `192.168.2.150` |
| **No sensor data** | Turn on Pulse Oximeter, check BLE service |
| **Temi doesn't speak** | Check Temi volume settings |

---

## ✅ Success Criteria

Your system is fully operational if:
1. ✅ All 4 Pi services are `active (running)`
2. ✅ `/health` endpoint returns `{"status":"healthy"}`
3. ✅ BLE monitor logs show sensor connections
4. ✅ App launches and shows "Connected"
5. ✅ Voice query gets an AI response
6. ✅ Sensor reading appears in the app

---

## 📞 If Everything Fails

Run this master diagnostic:
```bash
ssh openhabian@192.168.2.150 << 'EOF'
echo "=== Services ==="
systemctl is-active openhab mosquitto healthub-ble healthub-agent
echo ""
echo "=== Agent Health ==="
curl -s http://localhost:8000/health
echo ""
echo "=== Recent Agent Logs ==="
journalctl -u healthub-agent -n 10 --no-pager
EOF
```

Copy the output and review it for errors.
