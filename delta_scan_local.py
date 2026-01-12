import asyncio
from bleak import BleakScanner

async def scan(duration=5.0):
    print(f"   Scanning for {duration}s...")
    devices = await BleakScanner.discover(return_adv=True, timeout=duration)
    return {d.address: (d, adv) for d, adv in devices.values()}

async def main():
    print("--- STEP 1: Turn Device OFF (or move far away) ---")
    input("Press Enter when ready...")
    baseline = await scan()
    print(f"   Captured {len(baseline)} baseline devices.")

    print("\n--- STEP 2: Turn Device ON (put in pairing mode) ---")
    input("Press Enter immediately after turning it on...")
    active = await scan()
    
    print("\n--- RESULTS: Potential Candidates ---")
    found_new = False
    for mac, (d, adv) in active.items():
        # If not in baseline, OR signal jumped significantly
        if mac not in baseline:
            print(f"🎯 NEW: {mac} | {d.name} | RSSI: {adv.rssi}")
            found_new = True
        else:
            old_rssi = baseline[mac][1].rssi
            if adv.rssi > old_rssi + 15: # Signal jump > 15dBm
                print(f"📈 JUMP: {mac} | {d.name} | {old_rssi}->{adv.rssi}")
                found_new = True
    
    if not found_new:
        print("No obvious new devices found. Try again?")

asyncio.run(main())
