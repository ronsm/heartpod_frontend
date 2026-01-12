import asyncio
from bleak import BleakScanner, BleakClient

TARGET = "FF:00:00:00:37:8C"

async def main():
    print("📡 Scanning for 5 seconds...")
    # Get device AND advertisement data
    data = await BleakScanner.discover(return_adv=True, timeout=5.0)
    
    target_device = None
    
    print("\n--- VISIBLE DEVICES ---")
    for mac, (d, adv) in data.items():
        mark = "🎯" if d.address == TARGET else "  "
        print(f"{mark} [{d.address}] {d.name or 'Unknown'} (RSSI: {adv.rssi})")
        if d.address == TARGET:
            target_device = d

    if target_device:
        print(f"\n✨ FOUND TARGET: {TARGET}")
        print("Attempting to connect and inspect details...")
        try:
            async with BleakClient(target_device.address) as client:
                print(f"✅ Connected! Paired: {client.is_connected}")
                print("\n--- SERVICES & CHARACTERISTICS ---")
                for service in client.services:
                    print(f"Service: {service.uuid} ({service.description})")
                    for char in service.characteristics:
                        print(f"  └─ Char: {char.uuid} | Props: {char.properties}")
        except Exception as e:
            print(f"❌ Connection Failed: {e}")
            print("(Try 'remove FF:00:00:00:37:8C' in bluetoothctl usually fixes this)")
    else:
        print(f"\n⚠️ Target {TARGET} not found. Is it turned ON?")

asyncio.run(main())
