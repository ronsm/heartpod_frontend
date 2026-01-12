import subprocess
import sys
import threading
import time

def read_output(process):
    print("👀 Agent Monitor Listening for PIN Requests...")
    print("   (Keep this window open! Run force_pair.py in another window!)")
    while True:
        line = process.stdout.readline()
        if not line:
            break
        decoded = line.decode('utf-8').strip()
        if not decoded:
            continue
            
        print(f"[CTL] {decoded}")
        
        if "Enter passkey" in decoded:
            print("\n" + "="*40)
            print("🚀 PIN REQUEST DETECTED!")
            print("="*40)
            pin = input("👉 ENTER PIN FROM THERMOMETER NOW: ")
            process.stdin.write(f"{pin}\n".encode('utf-8'))
            process.stdin.flush()
            
        if "Authorize service" in decoded:
             print("Auto-authorizing service...")
             process.stdin.write("yes\n".encode('utf-8'))
             process.stdin.flush()

def main():
    print("🚀 Starting Agent Monitor...")
    cmd = ["bluetoothctl"]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=0)
    
    t = threading.Thread(target=read_output, args=(proc,), daemon=True)
    t.start()
    
    # Setup Agent
    commands = ["agent on", "default-agent", "agent KeyboardDisplay", "scan on", "discoverable on"]
    for c in commands:
        proc.stdin.write(f"{c}\n".encode('utf-8'))
        proc.stdin.flush()
        time.sleep(0.5)
        
    # Just wait forever
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        proc.terminate()

if __name__ == "__main__":
    main()
