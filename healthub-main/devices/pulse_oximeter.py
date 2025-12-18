# devices/pulse_oximeter.py
import asyncio
from bleak import BleakClient
from .base_device import Device

# UUID
NOTIFY_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"

class PulseOximeter(Device):
    """Represents a generic Pulse Oximeter."""

    def _parse_live_data(self, sender, data):
        """Parses live oximeter data and sends it to OpenHAB."""
        if data.hex().startswith("ff44") and len(data) >= 6:
            spo2 = data[4]
            pulse = data[5]
            if pulse > 0 and spo2 > 0:
                print(f"    🩸 Oximeter: SpO2={spo2}%, Pulse={pulse} bpm")
                self._update_data({"spo2": spo2, "pulse": pulse})

    async def _start_monitoring_notifications(self, client: BleakClient):
        """Starts the notifications for continuous monitoring."""
        print("    Subscribed to Oximeter notifications.")
        await client.start_notify(NOTIFY_CHAR_UUID, self._parse_live_data)
    
    # This method is no longer used but required by the base class structure.
    # It can be removed if you also remove it from the base class.
    async def check_device(self):
        pass