import asyncio
from bleak import BleakClient

# Top 2 candidates + a few others just in case
CANDIDATES = [
    "28:DE:65:2D:B6:9E"
]

THERMOMETER_UUID = "00001809"

async def check(mac):
    print(f"\n👉 Checking {mac}...")
    try:
        async with BleakClient(mac, timeout=10.0) as client:
            print(f"   ✅ Connected. Scanning services...")
            found_thermometer = False
            for service in client.services:
                if THERMOMETER_UUID in str(service.uuid):
                    print(f"   🌡️ FOUND THERMOMETER SERVICE! (0x1809)")
                    found_thermometer = True
                else:
                    print(f"      - Service: {service.uuid}")
            
            if found_thermometer:
                print(f"   🎉🎉 MATCH FOUND: {mac} is the Thermometer! 🎉🎉")
                return True
    except Exception as e:
        print(f"   ❌ Failed: {e}")
    return False

async def main():
    print("Checking candidates for Health Thermometer Service (0x1809)...")
    for mac in CANDIDATES:
        if await check(mac):
            break
    print("\nDone.")

asyncio.run(main())
