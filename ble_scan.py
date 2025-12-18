import asyncio
from bleak import BleakScanner

async def scan():
    print("Scanning for BLE devices for 10 seconds...")
    devices = await BleakScanner.discover(timeout=10.0)
    
    print(f"\nFound {len(devices)} devices:")
    print("-" * 50)
    print(f"{'Address':<20} | {'Name'}")
    print("-" * 50)
    
    for d in devices:
        name = d.name or "Unknown"
        print(f"{d.address:<20} | {name}")
        
    print("-" * 50)

if __name__ == "__main__":
    try:
        asyncio.run(scan())
    except Exception as e:
        print(f"Error: {e}")
        print("Try running with sudo if you encounter permission errors.")
