#!/usr/bin/env python3
"""
Beurer FT95 Thermometer → OpenHAB Bridge
Handles disconnections and auto-reconnects
"""
import asyncio
import struct
import requests
from bleak import BleakClient, BleakScanner
import logging
from datetime import datetime

# Configuration
MAC = "FF:00:00:00:37:8C"
UUID_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb"
OPENHAB_URL = "http://localhost:8080/rest/items"
ITEM_TEMP = "Thermometer_Temperature"
ITEM_LAST_USE = "Thermometer_LastUse"

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def ieee11073_float(value_bytes):
    """Decode IEEE-11073 32-bit float"""
    raw = struct.unpack('<I', value_bytes)[0]
    mantissa = raw & 0x00FFFFFF
    exponent = (raw >> 24) & 0xFF
    
    if mantissa >= 0x800000:
        mantissa = -((0xFFFFFF + 1) - mantissa)
    if exponent >= 0x80:
        exponent = -((0xFF + 1) - exponent)
        
    return mantissa * (10 ** exponent)

def notification_handler(sender, data):
    """Handle temperature notifications"""
    try:
        flags = data[0]
        is_fahrenheit = (flags & 0x01)
        
        temp_bytes = data[1:5]
        temperature = ieee11073_float(temp_bytes)
        
        if is_fahrenheit:
            temperature = (temperature - 32) * 5/9
            
        logger.info(f"🌡️  Temperature: {temperature:.1f} °C")
        
        # Post to OpenHAB
        try:
            requests.post(
                f"{OPENHAB_URL}/{ITEM_TEMP}", 
                data=f"{temperature:.1f}", 
                headers={'Content-Type': 'text/plain'},
                timeout=2
            )
            now = datetime.now().isoformat()
            requests.post(
                f"{OPENHAB_URL}/{ITEM_LAST_USE}", 
                data=now, 
                headers={'Content-Type': 'text/plain'},
                timeout=2
            )
            logger.info("   ✅ Sent to OpenHAB")
        except Exception as e:
            logger.error(f"   ⚠️  OpenHAB Error: {e}")
    except Exception as e:
        logger.error(f"Error processing notification: {e}")

async def connect_and_listen():
    """Connect to thermometer and listen for data"""
    while True:
        try:
            logger.info(f"🔍 Scanning for thermometer {MAC}...")
            device = await BleakScanner.find_device_by_address(MAC, timeout=10.0)
            
            if not device:
                logger.warning("⚠️  Thermometer not found, retrying in 10s...")
                await asyncio.sleep(10)
                continue
            
            logger.info(f"📡 Found thermometer, connecting...")
            
            async with BleakClient(device, timeout=20.0) as client:
                logger.info(f"✅ Connected!")
                
                # Start notifications
                await client.start_notify(UUID_MEASUREMENT, notification_handler)
                logger.info("👂 Listening for temperature data...")
                
                # Keep connection alive
                while client.is_connected:
                    await asyncio.sleep(1)
                    
                logger.warning("⚠️  Disconnected from thermometer")
                
        except Exception as e:
            logger.error(f"❌ Error: {e}")
            logger.info("Retrying in 10 seconds...")
            await asyncio.sleep(10)

async def main():
    """Main loop with auto-reconnect"""
    logger.info("🚀 Starting Thermometer Service")
    logger.info(f"   Device: {MAC}")
    logger.info(f"   OpenHAB: {OPENHAB_URL}")
    
    await connect_and_listen()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("\n👋 Shutting down gracefully...")
