#!/usr/bin/env python3
"""
Mock backend for HealthHub Android app (WebSocket version).

Pushes {"type": "state", "page_id": N, "data": {...}} to all connected clients
whenever you change page. Prints any action messages received from the app.

Usage:
    python3 mock_backend.py
    python3 mock_backend.py --port 8000
"""

import argparse
import asyncio
import json
import threading
from typing import Optional, Set

import websockets
from websockets.server import WebSocketServerProtocol

# ---------------------------------------------------------------------------
# Page definitions — mirrors the real backend's _build_data() output
# ---------------------------------------------------------------------------
PAGES = {
    1:  {"name": "IDLE", "data": {}},
    2:  {"name": "WELCOME", "data": {
        "message": (
            "I'm Temi, your digital health assistant. I'll guide you step-by-step "
            "through the self-screening process and provide you with a copy of your "
            "results to take away.\n\nBefore we start, please take a seat and make "
            "yourself comfortable. I will ask a few general lifestyle questions to "
            "give the clinical team some background. You can choose to skip any "
            "question if you wish.\n\nLet me know if you wish to continue."
        )
    }},
    3:  {"name": "Q1 (smoking)", "data": {
        "question": "Q1. How frequently do you smoke?",
        "options": json.dumps([
            "I previously smoked but no longer do",
            "I do not and have never smoked",
            "Occasionally (e.g. weekly or monthly)",
            "A few times a day",
            "Many times per day",
        ]),
    }},
    4:  {"name": "Q2 (exercise)", "data": {
        "question": "Q2. How often do you exercise?",
        "options": json.dumps([
            "Never",
            "Rarely (a few times a month)",
            "Sometimes (1-2 times a week)",
            "Often (3-4 times a week)",
            "Daily",
        ]),
    }},
    5:  {"name": "Q3 (alcohol)", "data": {
        "question": "Q3. How many units of alcohol do you drink per week?",
        "options": json.dumps([
            "None",
            "1-7 units",
            "8-14 units",
            "15-21 units",
            "More than 21 units",
        ]),
    }},
    6:  {"name": "MEASURE_INTRO", "data": {
        "message": (
            "Great, thank you for answering those questions! "
            "Now we'll take three quick measurements: an oximeter reading, "
            "a blood pressure reading, and your weight. "
            "Just say 'continue' when you're happy to begin."
        )
    }},
    7:  {"name": "OXIMETER_INTRO",   "data": {"device": "oximeter"}},
    8:  {"name": "OXIMETER_READING", "data": {"message": "Taking oximeter reading... please stay still."}},
    9:  {"name": "OXIMETER_DONE",    "data": {"value": "HR: 72 bpm  /  SpO2: 98%", "unit": ""}},
    10: {"name": "BP_INTRO",         "data": {"device": "blood pressure monitor"}},
    11: {"name": "BP_READING",       "data": {"message": "Measuring now. Please relax and keep still."}},
    12: {"name": "BP_DONE",          "data": {"value": "120 / 80", "unit": "mmHg"}},
    13: {"name": "SCALE_INTRO",      "data": {"device": "scale"}},
    14: {"name": "SCALE_READING",    "data": {"message": "Taking weight reading... please stand still."}},
    15: {"name": "SCALE_DONE",       "data": {"value": "72.4", "unit": "kg"}},
    16: {"name": "RECAP", "data": {
        "q1": "I do not and have never smoked",
        "q2": "Often (3-4 times a week)",
        "q3": "None",
        "oximeter": "72 bpm / 98%",
        "bp": "125/82 mmHg",
        "weight": "72.4 kg",
    }},
    17: {"name": "SORRY", "data": {"message": "Sorry, we weren't able to get a reading."}},
}

# ---------------------------------------------------------------------------
# WebSocket server state (shared between threads)
# ---------------------------------------------------------------------------
_clients: Set[WebSocketServerProtocol] = set()
_ws_state: dict = {"page_id": 1, "data": {}}
_loop: Optional[asyncio.AbstractEventLoop] = None
_state_lock = threading.Lock()


async def _handler(websocket: WebSocketServerProtocol):
    _clients.add(websocket)
    try:
        # Send current state immediately on connect
        with _state_lock:
            current = dict(_ws_state)
        await websocket.send(json.dumps({"type": "state", **current}))

        async for raw in websocket:
            try:
                msg = json.loads(raw)
                action = msg.get("action", "?")
                data = msg.get("data", {})
                print(f"\n  [app] action='{action}'" + (f"  data={data}" if data else ""))
                _print_prompt()
            except (json.JSONDecodeError, Exception):
                pass
    except websockets.ConnectionClosed:
        pass
    finally:
        _clients.discard(websocket)


async def _broadcast(message: str):
    if _clients:
        await asyncio.gather(
            *(c.send(message) for c in set(_clients)),
            return_exceptions=True,
        )


async def _serve(port: int):
    async with websockets.serve(_handler, "0.0.0.0", port):
        await asyncio.Future()  # run until cancelled


# ---------------------------------------------------------------------------
# Terminal helpers
# ---------------------------------------------------------------------------
def _print_pages():
    print("\nPages:")
    for pid, page in PAGES.items():
        print(f"  {pid:>2}  {page['name']}")
    print()


def _print_prompt():
    with _state_lock:
        pid = _ws_state["page_id"]
    name = PAGES.get(pid, {}).get("name", "?")
    print(f"Current page: {pid} ({name})  — type a page ID to switch, or 'q' to quit")
    print("> ", end="", flush=True)


def _set_page(page_id: int):
    page = PAGES.get(page_id)
    if not page:
        print(f"  Unknown page ID: {page_id}")
        return
    with _state_lock:
        _ws_state["page_id"] = page_id
        _ws_state["data"] = dict(page["data"])
    msg = json.dumps({"type": "state", "page_id": page_id, "data": page["data"]})
    if _loop and _loop.is_running():
        asyncio.run_coroutine_threadsafe(_broadcast(msg), _loop)
    print(f"  -> {page_id} ({page['name']})")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    global _loop

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    _loop = asyncio.new_event_loop()

    def _run():
        asyncio.set_event_loop(_loop)
        _loop.run_until_complete(_serve(args.port))

    thread = threading.Thread(target=_run, daemon=True, name="ws-server")
    thread.start()

    print(f"Mock backend running on ws://0.0.0.0:{args.port}")
    _print_pages()
    _print_prompt()

    try:
        while True:
            line = input().strip()
            if line.lower() in ("q", "quit", "exit"):
                break
            if line.isdigit():
                _set_page(int(line))
            elif line:
                print("  Type a page number or 'q' to quit.")
            _print_prompt()
    except (KeyboardInterrupt, EOFError):
        pass

    print("\nShutting down.")
    _loop.call_soon_threadsafe(_loop.stop)


if __name__ == "__main__":
    main()
