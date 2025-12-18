import asyncio
from bleak import BleakScanner, BleakClient, BleakError
import sys
from datetime import datetime
import struct

DEVICE_ADDRESS = "F0:A1:62:ED:E6:A9"

# --- UUIDs ---
BP_MEASUREMENT_CHAR_UUID = "00002a35-0000-1000-8000-00805f9b34fb"
CURRENT_TIME_CHAR_UUID   = "00002a2b-0000-1000-8000-00805f9b34fb"
OMRON_STATUS_CHAR_UUID   = "b305b680-aee7-11e1-a730-0002a5d5c51b"
OMRON_WRITE_CHAR_UUID    = "db5b55e0-aee7-11e1-965e-0002a5d5c51b"

# --- Data Handlers ---
def sfloat_to_float(sfloat_val):
    exponent = sfloat_val >> 12
    mantissa = sfloat_val & 0x0FFF
    if exponent >= 8: exponent = -((0x000F + 1) - exponent)
    if mantissa >= 2048: mantissa = -((0x0FFF + 1) - mantissa)
    return mantissa * (10 ** exponent)

def bp_measurement_handler(sender, data):
    print("\n--- ✅ New Blood Pressure Reading Received ---")
    flags = data[0]
    units = "kPa" if flags & 0x01 else "mmHg"
    systolic = round(sfloat_to_float(int.from_bytes(data[1:3], "little")), 1)
    diastolic = round(sfloat_to_float(int.from_bytes(data[3:5], "little")), 1)
    print(f"🩸 Systolic: {systolic} {units}")
    print(f"🩸 Diastolic: {diastolic} {units}")
    print("-" * 41 + "\n")

def status_handler(sender, data):
    print(f"  [Device Status Update]: Raw data: {data.hex()}")

# --- Main Connection Logic ---
async def run_final_listener():
    print("--- OMRON Listener (Final Version) ---")
    print("Ensure device is awake by taking a measurement or pressing the sync button.")

    try:
        print(f"Scanning for {DEVICE_ADDRESS} to ensure it's available...")
        device = await BleakScanner.find_device_by_address(DEVICE_ADDRESS, timeout=10.0)
        if not device:
            print("❌ Device not found. Please wake it up and run the script again.")
            return

        print(f"✅ Device found! Connecting...")
        async with BleakClient(device, timeout=20.0) as client:
            print(f"✅ Connection successful!")

            # The client.pair() call is REMOVED. The OS now handles the trusted relationship.
            
            print("Subscribing to OMRON status notifications...")
            await client.start_notify(OMRON_STATUS_CHAR_UUID, status_handler)
            
            print("Subscribing to Blood Pressure notifications...")
            await client.start_notify(BP_MEASUREMENT_CHAR_UUID, bp_measurement_handler)
            
            print("Sending 'start' command...")
            await client.write_gatt_char(OMRON_WRITE_CHAR_UUID, b'\x01\x00', response=True)
            
            now = datetime.now()
            print(f"Setting device time to: {now.strftime('%Y-%m-%d %H:%M:%S')}")
            time_payload = struct.pack('<HBBBBBBBB', now.year, now.month, now.day, now.hour, now.minute, now.second, now.weekday() + 1, 0, 0)
            await client.write_gatt_char(CURRENT_TIME_CHAR_UUID, time_payload)

            print("\n✅ Handshake complete. Listening for measurements for 5 minutes...")
            await asyncio.sleep(300)

    except Exception as e:
        print(f"\n❌ An error occurred: {e}")

if __name__ == "__main__":
    asyncio.run(run_final_listener())
