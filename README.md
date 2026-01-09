# HealthHub: AI-Powered Patient Screening Robot

HealthHub is a seamless, voice-controlled patient screening application for the **Temi Robot**. It guides patients through health questionnaires and physical measurements (like heart rate and oxygen levels) using a hybrid AI architecture.

Everything is containerized! You don't need to install Android Studio or SDKs to build or test the system.

### Prerequisites
- Docker installed (for build/emulator)
- Linux with X11 (for emulator display)
- WiFi network connecting Temi, Raspberry Pi, and Development PC.

---

## System Architecture
The system consists of three main tiers:
1.  **Temi Robot (Frontend):** Android app handling UI, Voice capture, and Navigation.
2.  **Raspberry Pi 2 (Edge):** AI Logic (LangGraph), OpenHAB (Data storage), and BLE Monitoring.
3.  **Groq (Cloud AI):** High-speed LLM (Llama 3.1) for natural language reasoning.

For a deep dive, see:
*   [System Architecture Guide](SYSTEM_ARCHITECTURE_GUIDE.md)
*   [SDK & API Reference](DATA_FLOW_REFERENCE.md)
*   [State Machine Logic](STATE_MACHINE_GUIDE.md)
*   [Codebase Map](CODEBASE_MAP.md)

---

## Quick Start: How to Run

### 1. Start the Backend (Raspberry Pi)
The Pi manages the AI "brain" and data persistence.
```bash
ssh pi@192.168.2.150
cd ~/healthub-agent
export GROQ_API_KEY=your_key_here
./start_agent.sh
```
*Verify: Visit `http://192.168.2.150:8000/docs` in your browser.*

### 2. Run the App (Temi Robot)
Deploy the frontend to the robot over WiFi.
```bash
./gradlew assembleDebug
./deploy_to_temi.sh
```
*Note: Ensure your PC and Temi are on the same WiFi network.*

### 3. Run on Emulator (Testing only)
If you don't have a robot, use the containerized emulator:
```bash
./run-emulator.sh
```

---

## Voice Interaction
HealthHub features a **Silent "Hey Temi" Bypass**. Once the screening starts, the robot automatically opens its microphone at every step. You do not need to say "Hey Temi" or touch the screen during the flow.

**Common Commands:**
*   *"Start screening"*
*   *"Yes / Next"*
*   *"Cancel / Stop"*

---

## Project Structure
```
android-healthub-main/
├── app/                    # Android (Kotlin) Source
├── langraph-backend/       # AI Agent (Python/LangGraph)
├── Dockerfile              # Build environment
├── run-emulator.sh         # One-click testing
├── deploy_to_temi.sh       # WiFi deployment script
└── DOCS/                   # Detailed technical guides
```

---

## Configuration
*   **Temi IP:** `192.168.2.115`
*   **Pi IP:** `192.168.2.150` (OpenHAB & AI Gateway)
*   **Database:** OpenHAB REST API (Running on Pi)

---

## Troubleshooting
*   **"Pi Unreachable":** Verify the Pi is online at `.150`.
*   **No Voice Response:** Check the volume on Temi's top bar and ensure Groq API key is valid.
*   **BLE Disconnected:** Ensure the Oximeter is on and within range of the Pi.

---
2026 HealthHub Team
