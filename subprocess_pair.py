import subprocess
import sys
import time
import re
import threading

MAC = "FF:00:00:00:37:8C"

def read_output(process):
    """Continuously read output from bluetoothctl and handle prompts."""
    while True:
        line = process.stdout.readline()
        if not line:
            break
        
        decoded = line.decode('utf-8').strip()
        if not decoded:
            continue
            
        print(f"[CTL] {decoded}")
        
        # Check for PIN detection
        # Common prompt: [agent] Enter passkey (123456):
        if "Enter passkey" in decoded:
            print("\n" + "="*40)
            print("🚀 PIN REQUEST DETECTED!")
            print("="*40)
            pin = input("👉 ENTER PIN FROM THERMOMETER NOW: ")
            process.stdin.write(f"{pin}\n".encode('utf-8'))
            process.stdin.flush()
            
        # Check for successful pair
        if "Pairing successful" in decoded:
            print("\n🎉 PAIRING SUCCESSFUL! YOU WIN!")
            sys.exit(0)
            
        # Check for Authentication Failed
        if "AuthenticationFailed" in decoded or "AuthenticationRejected" in decoded:
            print("\n⚠️ AUTH FAILED. TRY AGAIN (Reset device?).")

def main():
    print(f"🚀 Starting Subprocess Pair for {MAC}")
    
    # Start bluetoothctl process
    cmd = ["bluetoothctl"]
    proc = subprocess.Popen(
        cmd, 
        stdin=subprocess.PIPE, 
        stdout=subprocess.PIPE, 
        stderr=subprocess.PIPE,
        bufsize=0 # Unbuffered
    )
    
    # Start reader thread
    t = threading.Thread(target=read_output, args=(proc,), daemon=True)
    t.start()
    
    # Send setup commands
    commands = [
        "agent on",
        "default-agent",
        "agent KeyboardDisplay",
        "scan on" # Start scanning
    ]
    
    for c in commands:
        actions = f"{c}\n"
        proc.stdin.write(actions.encode('utf-8'))
        proc.stdin.flush()
        time.sleep(0.5)
        
    print("\n👀 Scanning... Please WAKE UP the device now!")
    print("   (Wait until you see [NEW] or RSSI updates)")
    
    # Loop to check state and trigger pair
    # We will let the user trigger Pair manually by pressing Enter?
    # Or auto-trigger?
    # Let's auto-trigger every few seconds if we suspect it's there.
    
    print("Press ENTER to send PAIR command (do this when you see the device!)")
    while True:
        try:
            input() # Wait for user
            print(f"⚡ Sending PAIR {MAC}...")
            proc.stdin.write(f"pair {MAC}\n".encode('utf-8'))
            proc.stdin.flush()
        except KeyboardInterrupt:
            break
            
    proc.terminate()

if __name__ == "__main__":
    main()
