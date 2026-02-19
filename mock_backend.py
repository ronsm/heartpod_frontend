#!/usr/bin/env python3
"""
Mock backend for HealthHub Android app.
Serves the current page to the app and lets you change it by typing a page ID.

Usage:
    python3 mock_backend.py
    python3 mock_backend.py --port 8001
"""

import argparse
import json
import threading
from http.server import BaseHTTPRequestHandler, HTTPServer

# ---------------------------------------------------------------------------
# Page definitions — default data sent with each page
# ---------------------------------------------------------------------------
PAGES = {
    1:  {"name": "IDLE",  "data": {}},
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
        "options": [
            "I previously smoked but no longer do",
            "I do not and have never smoked",
            "Occasionally (e.g. weekly or monthly)",
            "A few times a day",
            "Many times per day",
        ],
    }},
    4:  {"name": "Q2 (exercise)", "data": {
        "question": "Q2. How often do you exercise?",
        "options": [
            "Never",
            "Rarely (a few times a month)",
            "Sometimes (1-2 times a week)",
            "Often (3-4 times a week)",
            "Daily",
        ],
    }},
    5:  {"name": "Q3 (alcohol)", "data": {
        "question": "Q3. How many units of alcohol do you drink per week?",
        "options": [
            "None",
            "1-7 units",
            "8-14 units",
            "15-21 units",
            "More than 21 units",
        ],
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
    9:  {"name": "OXIMETER_DONE",    "data": {"value": "98", "unit": "% SpO2"}},
    10: {"name": "BP_INTRO",         "data": {"device": "blood pressure monitor"}},
    11: {"name": "BP_READING",       "data": {"message": "Measuring now. Please relax and keep still."}},
    12: {"name": "BP_DONE",          "data": {"value": "120 / 80", "unit": "mmHg"}},
    13: {"name": "SCALE_INTRO",      "data": {"device": "scale"}},
    14: {"name": "SCALE_READING",    "data": {"message": "Taking weight reading... please stand still."}},
    15: {"name": "SCALE_DONE",       "data": {"value": "72.4", "unit": "kg"}},
    16: {"name": "RECAP",            "data": {}},
    17: {"name": "SORRY",            "data": {"message": "Sorry, we weren't able to get a reading."}},
}

# Current state — shared between HTTP thread and input thread
state = {"page_id": 1, "data": {}}
state_lock = threading.Lock()


# ---------------------------------------------------------------------------
# HTTP server
# ---------------------------------------------------------------------------
class Handler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path != "/state":
            self.send_response(404)
            self.end_headers()
            return
        with state_lock:
            body = json.dumps(state).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        if self.path != "/action":
            self.send_response(404)
            self.end_headers()
            return
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length)) if length else {}
        action = body.get("action", "?")
        data   = body.get("data", {})
        print(f"\n  [app] action='{action}'" + (f"  data={data}" if data else ""))
        print_prompt()
        self.send_response(200)
        self.end_headers()

    def log_message(self, *args):
        pass  # suppress request logs


# ---------------------------------------------------------------------------
# Terminal helpers
# ---------------------------------------------------------------------------
def print_pages():
    print("\nPages:")
    for pid, page in PAGES.items():
        print(f"  {pid:>2}  {page['name']}")
    print()

def print_prompt():
    with state_lock:
        pid = state["page_id"]
    name = PAGES.get(pid, {}).get("name", "?")
    print(f"Current page: {pid} ({name})  — type a page ID to switch, or 'q' to quit")
    print("> ", end="", flush=True)

def set_page(page_id: int):
    page = PAGES.get(page_id)
    if not page:
        print(f"  Unknown page ID: {page_id}")
        return
    with state_lock:
        state["page_id"] = page_id
        state["data"] = dict(page["data"])
    print(f"  -> {page_id} ({page['name']})")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    server = HTTPServer(("0.0.0.0", args.port), Handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    print(f"Mock backend running on port {args.port}")
    print_pages()
    print_prompt()

    try:
        while True:
            line = input().strip()
            if line.lower() in ("q", "quit", "exit"):
                break
            if line.isdigit():
                set_page(int(line))
            elif line:
                print("  Type a page number or 'q' to quit.")
            print_prompt()
    except (KeyboardInterrupt, EOFError):
        pass

    print("\nShutting down.")
    server.shutdown()


if __name__ == "__main__":
    main()
