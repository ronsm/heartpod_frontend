# HealthHub Codebase Map

This document explains where the different parts of the system live and what they are responsible for.

---

## 1. Android Application (`/app`)
*The "Face" of the system. Runs on the Temi robot's tablet.*

### Core Logic (`.../healthub/`)
| File / Folder | Purpose |
| :--- | :--- |
| **`MainActivity.kt`** | **The Orchestrator.** Manages the window lifecycle and registers all Temi listeners (Voice, Navigation). |
| **`fsm/StateMachine.kt`** | **The Logic Brain.** Defines the questionnaire steps (States) and what happens when you say "Yes" (Transitions). |
| **`nlu/IntentParser.kt`** | **The Fast Matcher.** Uses local patterns to instantly recognize "Start" or "Yes" without using the internet. |
| **`api/`** | **Communications.** Contains Retrofit code to talk to the Raspberry Pi backend. |
| **`speech/`** | **Microphone.** Logic for the "Always-On" listening behavior and native Android mic access. |

### User Interface (`.../healthub/ui/`)
| File | Screen Name | Description |
| :--- | :--- | :--- |
| `WelcomeScreen.kt` | **Home** | The initial "Tap to Speak" or "Start" screen. |
| `DeviceInstructionScreen.kt` | **Guide** | Shows the patient how to use the Oximeter. |
| `ReadingDisplayScreen.kt` | **Results** | Displays the heart rate and oxygen levels from the device. |
| `LoadingScreen.kt` | **Transition** | Shown while the robot is moving or the AI is "thinking". |

---

## 2. AI Backend (`/langraph-backend`)
*The "Intelligence" of the system. Runs on the Raspberry Pi 2.*

| File | Purpose |
| :--- | :--- |
| **`agent.py`** | **The AI Brain.** Contains the LangGraph configuration and prompts for Groq (Llama 3). |
| **`main.py`** | **The Gateway.** A FastAPI server that listens for voice text from the robot and sends it to the agent. |
| **`requirements.txt`** | **Dependencies.** Python libraries needed (langchain, fastapi, groq). |

---

## 3. Automation Scripts (Root Directory)
*Tools to help developers manage the robot.*

| Script | Action |
| :--- | :--- |
| **`deploy_to_temi.sh`** | Pushes the compiled Android app to the robot over WiFi. |
| **`restart_agent.sh`** | Quickly restarts the AI backend on the Raspberry Pi. |
| **`diagnose_voice.sh`** | Checks if the microphone and ASR services are responding correctly. |

---

## 4. Documentation Strategy
| File | Audience |
| :--- | :--- |
| `SETUP_AND_RUN_GUIDE.md` | **New Operators.** How to turn it on and run a screening. |
| `TECHNICAL_WORKFLOW.md` | **Engineers.** How the data moves from the Mic to the AI and back. |
| `VOICE_INTEGRATION_STATUS.md` | **Status.** Current progress on the voice features. |
