# 📱 How to Simulate Healthub Locally

Since the cloud environment blocks emulator network traffic, you can easily simulate the app on your local machine using your installed Android Studio.

## ✅ Prerequisites
- **Android Studio** installed on your machine
- **APK File**: Downloaded from `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔹 Option 1: Android Studio Emulator (Recommended)

### 1. Prepare the Emulator
1. Open **Android Studio**.
2. Click on the **Device Manager** icon (usually on the right sidebar or top toolbar).
   - *Icon looks like a phone with an Android robot.*
3. If you don't have a device listed:
   - Click **Create Device**.
   - Choose **Phone** -> **Pixel 5** (or similar).
   - Click **Next**.
   - Select a System Image (e.g., **API 33** or **Tiramisu**). Download if needed.
   - Click **Next** -> **Finish**.
4. Click the **Play** (▶️) button next to your virtual device to start it.

### 2. Install the App
1. Locate the `app-debug.apk` file you downloaded.
2. Simply **drag and drop** the APK file onto the running emulator window.
   - You should see a message "Installing APK...".
3. Once installed, the app will appear in the app drawer or launch automatically.

### 3. Run & Test
1. Click the **Healthub** icon in the emulator.
2. The app should launch.
   - **Note**: Since it's running locally, it won't connect to the OpenHAB server unless your machine has access to `192.168.2.198` (the Pi).
   - If you are on the same network as the Pi, it might work!

---

## 🔹 Option 2: Physical Android Device

If you have an Android phone, this is often faster.

1. **Enable Developer Options**:
   - Settings -> About Phone -> Tap **Build Number** 7 times.
2. **Enable USB Debugging**:
   - Settings -> System -> Developer Options -> Toggle **USB Debugging** on.
3. **Connect via USB**:
   - Plug your phone into your computer.
   - Accept the "Allow USB debugging?" prompt on your phone.
4. **Install**:
   - Open a terminal on your local machine.
   - Run: `adb install path/to/app-debug.apk`
   - *Or just copy the APK to your phone's storage and tap it to install (you may need to allow "Install from Unknown Sources").*

---

## ⚠️ Critical Troubleshooting: System Crashes / GPU Errors

If your computer **crashes** or you see **GPU driver errors** when starting the emulator:

### ✅ Fix: Switch to Software Rendering
This disables hardware acceleration, which is safer for your system (though slightly slower).

1. Open **Device Manager** in Android Studio.
2. Click the **Edit** (pencil icon ✏️) next to your virtual device.
3. Click **Show Advanced Settings**.
4. Scroll down to **Emulated Performance**.
5. Change **Graphics** from "Automatic" (or Hardware) to **"Software - GLES 2.0"**.
6. Click **Finish**.
7. Start the emulator again.

---

## ❓ Other Troubleshooting

**"App not installed" error?**
- Uninstall any previous version of Healthub first.

**App crashes on launch?**
- This might be due to network connectivity to the OpenHAB server.
- Check if your computer can reach the OpenHAB IP: `ping 192.168.2.198`

**Emulator is slow?**
- Ensure "Hardware Acceleration" is enabled in Android Studio settings.
