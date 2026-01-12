    # You'll need to find your oximeter's address and put it in the next script.
    # It might be listed as "OXIMETER" or a similar name.
    # The address will look like "CB:31:33:32:1F:8F".   
    # Device: OXIMETER, Address: CB:31:33:32:1F:8F
    # Device: Thermo 16, Address: 00:24:E4:AD:C8:17


import asyncio
from bleak import BleakScanner
from datetime import datetime

# This is a set to store unique device addresses we've seen
seen_devices = set()

async def run_scanner():
    """
    This function will scan for Bluetooth LE devices and print them as they are discovered.
    """
    print("Starting endless scan. Press Ctrl+C to stop.")
    
    # Define a callback function to handle each discovered device
    def detection_callback(device, advertisement_data):
        # We only want to print each device once
        if device.address not in seen_devices:
            seen_devices.add(device.address)
            print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Detected new device:")
            print(f"  Name: {device.name}")
            print(f"  Address: {device.address}")
            print(f"  RSSI: {advertisement_data.rssi}")
            print(f"  Services: {advertisement_data.service_uuids}")
            print(f"  Manufacturer Data: {advertisement_data.manufacturer_data}")
            print("-" * 20)

    # Use the BleakScanner with a detection callback
    # This will run in the background and call the callback for each advertisement packet
    scanner = BleakScanner()
    scanner.register_detection_callback(detection_callback)
    
    # Start the scanner
    await scanner.start()
    
    # Keep the script running indefinitely
    try:
        await asyncio.Event().wait()
    except asyncio.CancelledError:
        pass  # This will be raised when you press Ctrl+C
    finally:
        # Stop the scanner gracefully
        await scanner.stop()
        print("\nScan stopped.")

async def main():
    await run_scanner()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nScript interrupted by user. Exiting.")
