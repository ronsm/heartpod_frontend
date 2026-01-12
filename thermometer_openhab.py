import asyncio
import struct
import sys
import requests
from bleak import BleakScanner, BleakClient

# DEVICE CONFIG
MAC = "FF:00:00:00:37:8C"
OPENHAB_URL = "http://localhost:8080/rest/items"
ITEM_TEMP = "Thermometer_Temperature"
ITEM_LAST_USE = "Thermometer_LastUse"

# GATT UUIDs
UUID_THERMOMETER = "0000a21c-0000-1000-8000-00805f9b34fb" # Custom? No, standard is 0x1809 -> 0x2A1C.
# User log showed 00001809 service.
# Base UUID for BLE is 0000xxxx-0000-1000-8000-00805f9b34fb.
# Thermometer Measurement (0x2A1C)
UUID_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb"

def ieee11073_float(value_bytes):
    """
    Decodes IEEE-11073 32-bit float (24-bit mantissa, 8-bit exponent).
    Format: [Mantissa LSB] [Mantissa] [Mantissa MSB] [Exponent]
    """
    # Unpack as little-endian integer (4 bytes)
    raw = struct.unpack('<I', value_bytes)[0]
    
    mantissa = raw & 0x00FFFFFF
    exponent = (raw >> 24) & 0xFF
    
    # Handle sign of 24-bit mantissa
    if mantissa >= 0x800000:
        mantissa = -((0xFFFFFF + 1) - mantissa)
        
    # Handle sign of 8-bit exponent
    if exponent >= 0x80:
        exponent = -((0xFF + 1) - exponent)
        
    return mantissa * (10 ** exponent)

def notification_handler(sender, data):
    # Hex: 06 69 01 00 ff ...
    # Byte 0: Flags (06 -> 00000110 -> Celsius, Timestamp, Type)
    flags = data[0]
    is_fahrenheit = (flags & 0x01)
    
    # Temp is at index 1 (4 bytes)
    temp_bytes = data[1:5]
    temperature = ieee11073_float(temp_bytes)
    
    if is_fahrenheit:
        # Convert to C for consistency? Or keep F? HealthHub usually uses C.
        temperature = (temperature - 32) * 5/9
        
    print(f"🌡️ Decoded: {temperature:.1f} °C")
    
    # Post to OpenHAB
    try:
        # 1. Update Temperature
        requests.post(
            f"{OPENHAB_URL}/{ITEM_TEMP}", 
            data=f"{temperature:.1f}", 
            headers={'Content-Type': 'text/plain'}
        )
        # 2. Update Last Use
        from datetime import datetime
        now = datetime.now().isoformat()
        requests.post(
            f"{OPENHAB_URL}/{ITEM_LAST_USE}", 
            data=now, 
            headers={'Content-Type': 'text/plain'}
        )
        print("   ✅ Sent to OpenHAB!")
    except Exception as e:
        print(f"   ⚠️ OpenHAB Error: {e}")

async def main():
    print(f"🏥 HealthHub Thermometer Listener")
    print(f"👀 Connecting to {MAC}...")
    
    device = await BleakScanner.find_device_by_address(MAC, timeout=20.0)
    if not device:
        print("❌ Device not found.")
        return

    async with BleakClient(device) as client:
        print(f"🔗 Connected!")
        
        await client.start_notify(UUID_MEASUREMENT, notification_handler)
        
        print("⏳ Listening for measurements (Take a reading now!)...")
        # Keep alive for a bit to catch the burst
        await asyncio.sleep(30)
        
        await client.stop_notify(UUID_MEASUREMENT)

if __name__ == "__main__":
    asyncio.run(main())
