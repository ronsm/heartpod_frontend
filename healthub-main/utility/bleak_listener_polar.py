import asyncio
from bleak import BleakClient
import sys
from struct import unpack

# The MAC address of your Polar H10
DEVICE_ADDRESS = "A0:9E:1A:E3:63:A1"

## UUIDs for standard services and characteristics
HR_MEASUREMENT_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb"
BATTERY_LEVEL_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

## UUIDs for Polar-specific services and characteristics
# PMD Service: For measurement data (ECG, ACC, etc.)
PMD_SERVICE_UUID = "fb005c80-02e7-f387-1cad-8acd2d8df0c8"
# For controlling the measurement stream
PMD_CONTROL_UUID = "fb005c81-02e7-f387-1cad-8acd2d8df0c8"
# For receiving the measurement data
PMD_DATA_UUID = "fb005c82-02e7-f387-1cad-8acd2d8df0c8"

## --- Data Handlers ---

def handle_hr(sender, data):
    """
    Handles and decodes Heart Rate and RR Interval data.
    """
    # The first byte is a flags field
    flags = data[0]
    
    # Heart rate is in the second byte (UINT8 format)
    heart_rate = data[1]
    print(f"❤️ Heart Rate: {heart_rate} bpm")
    
    # Check if RR-Interval data is present (bit 4 of flags)
    if flags & 0b00010000:
        # RR-intervals are 16-bit unsigned integers (unit: 1/1024s)
        # The first RR-interval value starts at the 3rd byte (index 2)
        rr_intervals = []
        rr_offset = 2
        while rr_offset < len(data):
            # Read 2 bytes and convert to an integer
            rr_raw = int.from_bytes(data[rr_offset:rr_offset+2], byteorder="little")
            # Convert from 1/1024s unit to milliseconds
            rr_ms = (rr_raw / 1024.0) * 1000.0
            rr_intervals.append(round(rr_ms))
            rr_offset += 2
        
        print(f"    RR Intervals: {rr_intervals} ms")

def handle_battery(sender, data):
    """
    Handles and decodes Battery Level data.
    """
    # Battery level is a single byte (UINT8) representing the percentage
    battery_level = data[0]
    print(f"🔋 Battery Level: {battery_level}%")

def handle_acc(sender, data):
    """
    Handles and decodes Accelerometer (ACC) data from the PMD stream.
    """
    # The first byte indicates the data type (0x02 for ACC)
    if data[0] == 0x02:
        # Timestamp is the next 8 bytes
        # Samples follow the timestamp. Each sample is 3 signed 16-bit integers (X, Y, Z)
        frame_type = data[0]
        timestamp = int.from_bytes(data[1:9], byteorder="little")
        
        # We'll just decode and print the first sample set to avoid flooding the console
        sample_offset = 9
        if len(data) >= sample_offset + 6: # Check if there's at least one sample
            # Unpack 3 signed short integers (16-bit)
            x, y, z = unpack('<hhh', data[sample_offset:sample_offset+6])
            print(f"🏃 ACC Sample (X,Y,Z): {x}, {y}, {z} mG")
        else:
            print("Received an incomplete ACC data packet.")

## --- Main Connection Logic ---

async def run_listener():
    """
    Connects to the Polar H10, enables all data streams, and listens for notifications.
    """
    print(f"Attempting to connect to Polar H10 at {DEVICE_ADDRESS}...")
    try:
        async with BleakClient(DEVICE_ADDRESS) as client:
            if not client.is_connected:
                print(f"❌ Failed to connect.")
                return

            print(f"✅ Connected to Polar H10!")
            
            # 1. Subscribe to standard Heart Rate notifications
            print("Subscribing to Heart Rate notifications...")
            await client.start_notify(HR_MEASUREMENT_CHAR_UUID, handle_hr)

            # 2. Subscribe to standard Battery Level notifications
            print("Subscribing to Battery Level notifications...")
            await client.start_notify(BATTERY_LEVEL_CHAR_UUID, handle_battery)

            # 3. Enable and subscribe to Accelerometer (ACC) data stream
            print("Enabling Accelerometer (ACC) stream...")
            
            # This is the command to start the 52Hz ACC stream
            # It's a specific byte array that the Polar H10 understands
            acc_start_command = bytearray([0x02, 0x02, 0x00, 0x01, 0x34, 0x00, 0x01, 0x01, 0x10, 0x00, 0x02, 0x01, 0x08, 0x00])
            
            # Write the command to the PMD control characteristic
            await client.write_gatt_char(PMD_CONTROL_UUID, acc_start_command)
            
            # Now subscribe to the PMD data characteristic to receive the data
            await client.start_notify(PMD_DATA_UUID, handle_acc)
            print("✅ ACC stream enabled and listening.")

            # Keep the script running to listen for notifications
            print("\n--- Listening for all data. Press Ctrl+C to stop. ---")
            await asyncio.sleep(600)  # Listen for 10 minutes
            
            print("\nStopping notifications...")
            await client.stop_notify(HR_MEASUREMENT_CHAR_UUID)
            await client.stop_notify(BATTERY_LEVEL_CHAR_UUID)
            await client.stop_notify(PMD_DATA_UUID)
            print("Stopped listening.")

    except Exception as e:
        print(f"An error occurred: {e}")

async def main():
    await run_listener()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nProgram stopped by user.")
        sys.exit(0)
