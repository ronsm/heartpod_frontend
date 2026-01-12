import asyncio
import dbus
import dbus.service
import dbus.mainloop.glib
from gi.repository import GLib
from bleak import BleakScanner, BleakClient
import threading
import sys

# --- AGENT SETUP ---
BUS_NAME = 'org.bluez'
AGENT_INTERFACE = 'org.bluez.Agent1'
AGENT_PATH = "/test/agent"

class Agent(dbus.service.Object):
    @dbus.service.method(AGENT_INTERFACE, in_signature="", out_signature="")
    def Release(self):
        print("Agent Release")

    @dbus.service.method(AGENT_INTERFACE, in_signature="o", out_signature="u")
    def RequestPasskey(self, device):
        print(f"\n!!! REQUEST PASSKEY FOR {device} !!!")
        pin = input("👉 ENTER 6-DIGIT PIN FROM SCREEN: ")
        return dbus.UInt32(pin)

    @dbus.service.method(AGENT_INTERFACE, in_signature="ouq", out_signature="")
    def DisplayPasskey(self, device, passkey, entered):
        print(f"DisplayPasskey: {passkey}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="os", out_signature="")
    def DisplayPinCode(self, device, pincode):
        print(f"DisplayPinCode: {pincode}")

    @dbus.service.method(AGENT_INTERFACE, in_signature="o", out_signature="")
    def RequestAuthorization(self, device):
        print(f"RequestAuthorization: {device}")
        return

    @dbus.service.method(AGENT_INTERFACE, in_signature="", out_signature="")
    def Cancel(self):
        print("Agent Cancel")

def agent_loop():
    dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
    bus = dbus.SystemBus()
    capability = "KeyboardDisplay"
    
    # Register Agent
    obj = BUS_NAME + ".AgentManager1"
    manager = dbus.Interface(bus.get_object(BUS_NAME, "/org/bluez"), obj)
    
    # Clean up old agent if needed?
    try:
        manager.UnregisterAgent(AGENT_PATH)
    except:
        pass
        
    path = AGENT_PATH
    agent = Agent(bus, path)
    manager.RegisterAgent(path, capability)
    manager.RequestDefaultAgent(path)
    
    print("✅ Agent Registered (Background Thread)")
    
    mainloop = GLib.MainLoop()
    mainloop.run()

# --- BLEAK CLIENT SETUP ---
MAC = "FF:00:00:00:37:8C"

async def pair_device():
    # Start Agent in background thread
    t = threading.Thread(target=agent_loop, daemon=True)
    t.start()
    await asyncio.sleep(2) # Give agent time to start

    print(f"👀 Scanning for {MAC}...")
    device = await BleakScanner.find_device_by_address(MAC, timeout=20.0)
    
    if not device:
        print("❌ Device not found. WAKE IT UP!")
        return

    print(f"✅ Found! Connecting...")
    async with BleakClient(device) as client:
        print(f"🔗 Connected: {client.is_connected}")
        
        print("🔐 Initiating PAIRING...")
        try:
            # protection_level=2 requests bonding with authentication (PIN)
            res = await client.pair(protection_level=2)
            print(f"🎉 PAIRING RESULT: {res}")
            
            # Verify
            services = await client.get_services()
            print(f"   Services found: {len(services)}")
            
        except Exception as e:
            print(f"⚠️ Pairing Error: {e}")
            # If 0x0e, warn user
            if "0x0e" in str(e):
                print("\n⛔ FATAL ERROR: 0x0e (Unlikely Error)")
                print("   This means the Thermometer STILL has an old bond.")
                print("   You MUST perform a Factory Reset on the device itself.")

if __name__ == "__main__":
    try:
        asyncio.run(pair_device())
    except KeyboardInterrupt:
        pass
