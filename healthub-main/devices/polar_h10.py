# devices/polar_h10.py
import asyncio
from functools import partial
from bleak import BleakScanner, BleakClient
from .base_device import Device

# UUIDs
HR_MEASUREMENT_CHAR_UUID = "00002a37-0000-1000-8000-00805f9b34fb"
BATTERY_LEVEL_CHAR_UUID = "00002a19-0000-1000-8000-00805f9b34fb"

class PolarH10(Device):
    """Represents a Polar H10 Heart Rate sensor."""

    def _parse_live_hr_data(self, sender, data):
        """Parses live HR data and sends it immediately to OpenHAB."""
        flags = data[0]
        hr_is_uint16 = (flags & 1)
        
        offset = 2 if hr_is_uint16 else 1
        hr = int.from_bytes(data[1:1+offset], byteorder="little")
        
        if hr > 0:
            print(f"    ❤️ Polar HR: {hr} bpm")
            self._update_data({"heart_rate": hr})

    async def _start_monitoring_notifications(self, client: BleakClient):
        """Starts the notifications for continuous monitoring."""
        # Read the battery level once upon connection
        try:
            battery_data = await client.read_gatt_char(BATTERY_LEVEL_CHAR_UUID)
            battery_level = battery_data[0]
            print(f"    🔋 Polar Battery: {battery_level}%")
            self._update_data({"battery": battery_level})
        except Exception as e:
            print(f"    Could not read battery level: {e}")

        # Start listening for heart rate data
        await client.start_notify(HR_MEASUREMENT_CHAR_UUID, self._parse_live_hr_data)
        print("    Subscribed to HR notifications.")

    # The one-shot check_device is no longer the primary method
    async def check_device(self):
        pass