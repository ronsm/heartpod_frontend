# HealthHub: Setup & Operation Guide

This guide provides step-by-step instructions for a new person to get the HealthHub patient screening system up and running on the Temi robot.

## 1. System Overview

The system consists of three main parts:
1.  **Temi Robot:** Runs the Android App (Frontend).
2.  **Raspberry Pi 2:** Runs the AI Agent (LangGraph) and OpenHAB (Database).
3.  **Cloud AI:** The AI "brain" powered by **Groq**.

---

## 2. Prerequisites

Before you start, ensure you have:
*   **Temi Robot IP:** `192.168.2.115` (Should be static).
*   **Raspberry Pi IP:** `192.168.2.150`.
*   **Groq API Key:** Required for voice intelligence.
*   **WiFi:** All devices must be on the same network.

---

## 3. Step 1: Start the Backend (Raspberry Pi)

The Raspberry Pi handles the AI logic and data storage.

1.  **SSH into the Pi:**
    ```bash
    ssh pi@192.168.2.150
    ```
2.  **Start the AI Agent:**
    ```bash
    cd ~/healthub-agent
    export GROQ_API_KEY=your_key_here
    ./start_agent.sh
    ```
    *Verify:* Check that the service is running at `http://192.168.2.150:8000/docs`.

---

## 4. Step 2: Deploy the App (Temi Robot)

1.  **Connect your PC to the same WiFi.**
2.  **Build and Push the App:**
    ```bash
    cd [Project-Root]
    ./gradlew assembleDebug
    ./deploy_to_temi.sh
    ```
3.  **On the Temi Screen:**
    - The app should launch automatically.
    - **Permissions:** If prompted for "Microphone" permissions, select **"Allow"**.

---

## 5. Step 3: Running a Patient Screening

1.  **Start:** Say **"Start screening"** or tap the **"Tap to Speak"** button.
2.  **Voice Guidance:** The robot will say "Welcome to HealthHub...".
3.  **Oximeter:** The robot will move to the Oximeter station.
    - Follow the voice instructions.
    - The oximeter reading will automatically sync to the screen when you put it on your finger.
4.  **Confirm:** When the reading appears, say **"Yes"** or tap **"Confirm"**.

---

## 6. Troubleshooting

| Issue | Likely Cause | Solution |
| :--- | :--- | :--- |
| **"Heard: [wrong words]"** | Background noise | Move to a quieter area or speak closer to Temi's head. |
| **No "Heard" bubble** | App not listening | Tap "Tap to Speak" manually to resume. |
| **Robot says nothing** | Volume down | Increase Temi's system volume via the top-bar. |
| **"Pi Unreachable"** | IP Mismatch | Verify Pi is `.150`. |

---

## 7. Key Files for Developer Knowledge
*   [MainActivity.kt](file:///home/kwalker96/Downloads/android-healthub-main/app/src/main/java/org/hwu/care/healthub/MainActivity.kt): Core voice and UI logic.
*   [IntentParser.kt](file:///home/kwalker96/Downloads/android-healthub-main/app/src/main/java/org/hwu/care/healthub/nlu/IntentParser.kt): Command recognition patterns.
*   [technical_workflow.md](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/technical_workflow.md): Deep dive into the data flow.
