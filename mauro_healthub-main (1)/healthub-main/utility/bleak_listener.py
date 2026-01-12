import asyncio
from bleak import BleakClient
import sys

# Replace with your device's MAC address
DEVICE_ADDRESS = "CB:31:33:32:1F:8F" 

# The UUID of the characteristic that sends notifications
NOTIFY_CHARACTERISTIC_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

def notification_handler(sender, data):
    """
    Handles incoming notification data and decodes the payload.
    """
    # Convert the byte array to a hex string for easier inspection
    hex_data = data.hex()

    # The payload with the actual SpO2 and Pulse data appears to start with 'ff44'
    if hex_data.startswith("ff44"):
        print(f"[{sender}] - Decoded Data Payload: {hex_data}")
        
        # Based on your previous reverse-engineering:
        # SpO2 is the byte at offset 4
        # Pulse is the byte at offset 5
        try:
            # We can use data[4] and data[5] directly, since they are integers
            # representing the byte values.
            spo2 = data[4]
            pulse = data[5]
            
            # The pulse rate appears to have a bitmask to signify a valid reading.
            # A common pattern is to check if the high bit is set (e.g., bit 7).
            # If the value is > 127 (0x7f), it might be a special flag.
            # Let's assume for now a simple check. If pulse is 0, it's not a valid reading.
            if pulse > 0 and spo2 > 0:
                print(f"✅ DECODED: SpO2 = {spo2}%, Pulse Rate = {pulse} bpm")
                # Image of a pulse oximeter
            else:
                print(f"  Received data, but readings are not valid (SpO2: {spo2}, Pulse: {pulse}).")

        except IndexError:
            # This handles cases where the payload is shorter than expected
            print(f"  Payload too short to decode. Raw data: {hex_data}")
    else:
        # These are likely other types of data payloads, perhaps for waveform.
        print(f"[{sender}] - Other Data Payload: {hex_data}")
        
async def run_listener():
    print("Connecting and listening for notifications...")
    try:
        async with BleakClient(DEVICE_ADDRESS) as client:
            print("Connected! Now listening for notifications...")
            
            await client.start_notify(NOTIFY_CHARACTERISTIC_UUID, notification_handler)

            # Keep the script running to listen for notifications
            print("Listening for 10 minutes. Press Ctrl+C to stop.")
            await asyncio.sleep(600)  
            
            await client.stop_notify(NOTIFY_CHARACTERISTIC_UUID)
            print("Stopped listening.")

    except Exception as e:
        print(f"An error occurred: {e}")
        
async def main():
    await run_listener()

if __name__ == "__main__":
    asyncio.run(main())
