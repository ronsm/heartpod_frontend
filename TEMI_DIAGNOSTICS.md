# Temi App Diagnostic Report & Fixes

Based on your description, here are the issues and their solutions:

## 🔍 Issues Identified

### 1. Voice Not Responding
**Symptom:** When you say "Shall we start?", nothing happens.
**Root Cause:** The AI agent needs a Google API key to process voice input.

### 2. Navigation Not Working  
**Symptom:** App says "Going to oximeter" but Temi stands still.
**Root Cause:** Temi navigation requires location setup in the Temi Settings app.

### 3. BLE Reading Fails
**Symptom:** "Could not get the reading" after clicking "I'm Ready".
**Root Cause:** The Android app cannot directly access Bluetooth. It relies on the Pi's BLE service.

---

## ✅ Fixes

### Fix 1: Add Google API Key (CRITICAL)
```bash
ssh openhabian@192.168.2.150
nano ~/langraph-backend/.env
```

Add your key:
```
GOOGLE_API_KEY=AIza...your-key-here
```

Get a free key at: https://aistudio.google.com/apikey

Then restart:
```bash
sudo systemctl restart healthub-agent
```

---

### Fix 2: Set Up Temi Locations
1. On Temi, open **Temi Settings** app
2. Go to **"Locations"** or **"Map"**
3. Create a location called **"oximeter"**
4. Save the location where the Pulse Oximeter is placed

**Alternative:** If you don't want navigation, the app will still work - just ignore the "going to" message.

---

### Fix 3: Verify BLE Service is Scanning
The BLE service on the Pi should automatically detect your Pulse Oximeter.

**Check if it's working:**
```bash
ssh openhabian@192.168.2.150
journalctl -u healthub-ble -f
```

**Expected output when Oximeter is on:**
```
Connected to CB:31:33:32:1F:8F
Reading SpO2: 98
```

**If you see "Device not found":**
- Make sure the Pulse Oximeter is turned ON
- Check the MAC address matches in `~/healthub-main/config.py`

---

## 🧪 Testing the Fixes

### Test 1: Voice Interaction
1. Add the Google API key (Fix 1)
2. Restart the app on Temi
3. Say: "I need help"
4. **Expected:** Temi responds with AI-generated speech

### Test 2: BLE Reading
1. Turn ON the Pulse Oximeter
2. Wait 10 seconds (BLE service scans every 5s)
3. In the app, tap "I'm Ready"
4. **Expected:** Reading appears within 15 seconds

---

## 🚨 Quick Diagnostic Commands

Run these on your PC to check status:

```bash
# Check if AI agent is healthy
curl http://192.168.2.150:8000/health

# Check if BLE is scanning
ssh openhabian@192.168.2.150 "journalctl -u healthub-ble -n 20"

# Check Android app logs
adb logcat | grep -i "healthhub\|error"
```

---

## 📊 Current Status

| Component | Status | Issue |
|:----------|:-------|:------|
| Pi Services | ✅ Running | None |
| AI Agent | ⚠️ No API Key | Voice won't work |
| BLE Monitor | ✅ Running | Needs Oximeter ON |
| Temi Navigation | ⚠️ Not configured | Optional |
| Android App | ✅ Installed | Waiting for fixes |

---

## 🎯 Priority Actions

1. **NOW:** Add Google API key (5 min)
2. **OPTIONAL:** Set up Temi locations (10 min)
3. **TEST:** Turn on Oximeter and try reading (2 min)
