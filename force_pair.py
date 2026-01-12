import asyncio
from bleak import BleakScanner, BleakClient

MAC = "FF:00:00:00:37:8C"

async def main():
    print(f"👀 Scanning for {MAC}...")
    device = await BleakScanner.find_device_by_address(MAC, timeout=20.0)
    
    if not device:
        print("❌ Device not found. Please WAKE IT UP!")
        return

    print(f"✅ Found! Connecting...")
    async with BleakClient(device) as client:
        print(f"🔗 Connected: {client.is_connected}")
        
        print("🔐 Initiating PAIRING now...")
        try:
            # Force pairing
            res = await client.pair(protection_level=2)
            print(f"🎉 PAIRING RESULT: {res}")
            
            # If we get here, we are good!
            # List services to confirm
            services = await client.get_services()
            print(f"   Services found: {len(services)}")
            
        except Exception as e:
            print(f"⚠️ Pairing Error: {e}")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        pass
