# 🏗️ HealthHub: System Architecture & Data Flow

This guide provides a deep dive into how the HealthHub system is built, how the parts talk to each other, and how the voice interaction logic flows.

---

## 🏛️ Academic System Overview
This is a formal functional block diagram illustrating the multi-tier system architecture.

![Academic System Architecture](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/academic_system_architecture_diagram_1767892778240.png)

---

## 🔬 In-Depth Technical Sequence (Package & SDK Level)
The following formal sequence diagram illustrates the specific method calls across Android packages, the Temi SDK, and the FastAPI/LangGraph backend.

![In-Depth Technical Sequence](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/in_depth_system_sequence_diagram_1767892847819.png)

---

## 1. Physical Topology (The Network)
Connecting the Temi, Oximeter, Raspberry Pi, and Cloud AI.

![System Topology](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/healthub_system_architecture_1767892231874.png)

---

## 2. Sequence: The "Hey Temi" Bypass Loop
Technical chain for automatic microphone activation.

![Temi Bypass Sequence](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/sequence_temi_bypass_flow_1767892497753.png)

---

## 3. Sequence: AI Intent Parsing vs Local Matching
Logic flow for interpreting user speech.

![AI vs Local Decision Flow](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/sequence_ai_vs_local_decision_1767892511991.png)

---

## 4. Sequence: Physical Measurement Flow
Data journey from Oximeter to Database.

![Measurement Data Flow](file:///home/kwalker96/.gemini/antigravity/brain/ffae9911-3d52-4381-8543-f7096c488f66/sequence_measurement_data_flow_1767892528548.png)
