import asyncio
from bleak import BleakClient
import sys
from struct import unpack

# The MAC address of your Polar H10
DEVICE_ADDRESS = "A0:9E:1A:E3:63:A1"

## UUIDs
HR_MEASUREMENT_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb"
BATTERY_LEVEL_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb"
PMD_CONTROL_UUID = "fb005c81-02e7-f387-1cad-8acd2d8df0c8"
PMD_DATA_UUID = "fb005c82-02e7-f387-1cad-8acd2d8df0c8"

## --- Data Handlers ---

def handle_hr(sender, data):
    """
    Handles and decodes Heart Rate and RR Interval data with dynamic packet parsing.
    """
    # The first byte is the flags field, which describes the packet content
    flags = data[0]
    
    # Heart Rate Value Format (0 = UINT8, 1 = UINT16)
    hr_format_is_uint16 = (flags & 1)
    # Start offset for data after the flags byte
    offset = 1
    
    if hr_format_is_uint16:
        heart_rate = int.from_bytes(data[offset:offset+2], byteorder="little")
        offset += 2
    else:
        heart_rate = data[offset]
        offset += 1
        
    # Filter out invalid "zero" readings during sensor initialization
    if heart_rate == 0:
        print("⏳ Waiting for stable heart rate reading...")
        return
        
    print(f"❤️ Heart Rate: {heart_rate} bpm")
    
    # Sensor Contact Status (bit 2 and 1)
    # 0,1: No contact; 2: Contact not supported; 3: Contact detected
    contact_status = (flags >> 1) & 3
    if contact_status in [0, 1]:
        print("    Warning: Sensor contact is poor or not detected.")

    # Energy Expended Status (bit 3)
    energy_expended_present = (flags >> 3) & 1
    if energy_expended_present:
        # Energy expended is a UINT16, so we correctly skip its 2 bytes
        offset += 2
        
    # RR-Interval Status (bit 4)
    rr_interval_present = (flags >> 4) & 1
    if rr_interval_present:
        rr_intervals = []
        while offset < len(data):
            # RR-intervals are UINT16, 2 bytes each
            rr_raw = int.from_bytes(data[offset:offset+2], byteorder="little")
            # Unit is 1/1024s, we convert to milliseconds
            rr_ms = (rr_raw / 1024.0) * 1000.0
            rr_intervals.append(round(rr_ms))
            offset += 2
        
        if rr_intervals:
            print(f"    RR Intervals: {rr_intervals} ms")

def handle_battery(sender, data):
    battery_level = data[0]
    print(f"🔋 Battery Level: {battery_level}%")

def handle_acc(sender, data):
    if data[0] == 0x02: # ACC data type
        sample_offset = 9
        if len(data) >= sample_offset + 6:
            x, y, z = unpack('<hhh', data[sample_offset:sample_offset+6])
            print(f"🏃 ACC Sample (X,Y,Z): {x}, {y}, {z} mG")

## --- Main Connection Logic ---

async def run_listener():
    print(f"Attempting to connect to Polar H10 at {DEVICE_ADDRESS}...")
    try:
        async with BleakClient(DEVICE_ADDRESS) as client:
            if not client.is_connected:
                print("❌ Failed to connect.")
                return

            print(f"✅ Connected to Polar H10!")
            await asyncio.sleep(1.0)
            
            # Subscribe to all data streams
            await client.start_notify(HR_MEASUREMENT_CHAR_UUID, handle_hr)
            print("✅ Subscribed to HR.")
            await client.start_notify(BATTERY_LEVEL_CHAR_UUID, handle_battery)
            print("✅ Subscribed to Battery.")

            # Enable and subscribe to Accelerometer stream
            acc_command = bytearray([0x02, 0x02, 0x00, 0x01, 0x34, 0x00, 0x01, 0x01, 0x10, 0x00, 0x02, 0x01, 0x08, 0x00])
            await client.write_gatt_char(PMD_CONTROL_UUID, acc_command)
            await client.start_notify(PMD_DATA_UUID, handle_acc)
            print("✅ ACC stream enabled.")

            print("\n--- Listening for all data. Press Ctrl+C to stop. ---")
            await asyncio.sleep(600)

            print("\nStopping notifications...")
            await client.stop_notify(HR_MEASUREMENT_CHAR_UUID)
            await client.stop_notify(BATTERY_LEVEL_CHAR_UUID)
            await client.stop_notify(PMD_DATA_UUID)

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    try:
        asyncio.run(run_listener())
    except KeyboardInterrupt:
        print("\nProgram stopped by user.")
