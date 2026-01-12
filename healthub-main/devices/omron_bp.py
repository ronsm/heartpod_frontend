# devices/omron_bp.py
import asyncio
import struct
from datetime import datetime
from bleak import BleakClient
from .base_device import Device

# UUIDs
BP_MEASUREMENT_CHAR_UUID = "00002a35-0000-1000-8000-00805f9b34fb"
CURRENT_TIME_CHAR_UUID   = "00002a2b-0000-1000-8000-00805f9b34fb"
OMRON_WRITE_CHAR_UUID    = "db5b55e0-aee7-11e1-965e-0002a5d5c51b"

class OmronBP(Device):
    """Represents an OMRON Blood Pressure Monitor."""

    @staticmethod
    def _sfloat_to_float(sfloat_val):
        exponent = sfloat_val >> 12
        mantissa = sfloat_val & 0x0FFF
        if exponent >= 8: exponent = -((0x000F + 1) - exponent)
        if mantissa >= 2048: mantissa = -((0x0FFF + 1) - mantissa)
        return mantissa * (10 ** exponent)

    def _parse_bp_data(self, sender, data):
        """Parses BP data and sends it to OpenHAB."""
        systolic = round(self._sfloat_to_float(int.from_bytes(data[1:3], "little")), 1)
        diastolic = round(self._sfloat_to_float(int.from_bytes(data[3:5], "little")), 1)
        print(f"    🩸 OMRON: Systolic={systolic}, Diastolic={diastolic} mmHg")
        self._update_data({"systolic": systolic, "diastolic": diastolic})

    async def _start_monitoring_notifications(self, client: BleakClient):
        """Performs the handshake and starts notifications to get BP readings."""
        print("    Performing handshake and subscribing to BP notifications...")
        
        # Subscribe to notifications first
        await client.start_notify(BP_MEASUREMENT_CHAR_UUID, self._parse_bp_data)
        
        # Then perform the handshake to trigger the data transfer
        # Add a small delay to ensure notifications are ready
        await asyncio.sleep(0.5)
        
        now = datetime.now()
        time_payload = struct.pack('<HBBBBBBBB', now.year, now.month, now.day, now.hour, now.minute, now.second, now.weekday() + 1, 0, 0)
        await client.write_gatt_char(CURRENT_TIME_CHAR_UUID, time_payload)
        await client.write_gatt_char(OMRON_WRITE_CHAR_UUID, b'\x01\x00', response=True)
        print("    Handshake complete. Awaiting data...")

    async def monitor_device(self, ble_device):
        """
        Override base monitor_device to add retry logic.
        Omron BP monitors need to be in sync mode (user presses sync button) before connection.
        """
        disconnected_event = asyncio.Event()

        def disconnected_callback(client):
            print(f"  ⚠️ Device {client.address} disconnected.")
            disconnected_event.set()

        print(f"  Attempting to connect to {self.name} at {ble_device.address}...")
        self._update_status("ON")
        
        # Retry connection up to 3 times with delays
        max_retries = 3
        for attempt in range(max_retries):
            try:
                if attempt > 0:
                    print(f"  Retry attempt {attempt + 1}/{max_retries}...")
                    await asyncio.sleep(2)  # Wait 2 seconds between retries
                
                async with BleakClient(ble_device, disconnected_callback=disconnected_callback, timeout=15.0) as client:
                    if client.is_connected:
                        print(f"  ✅ Connected! Now monitoring {self.name} for live data...")
                        await self._start_monitoring_notifications(client)
                        # Wait for either disconnection or 60 seconds (enough time for one reading)
                        await asyncio.wait_for(disconnected_event.wait(), timeout=60.0)
                        print(f"  Session completed successfully.")
                        break  # Success, exit retry loop
                    else:
                        print(f"  ❌ Failed to connect to {self.name}.")
            except asyncio.TimeoutError:
                if attempt < max_retries - 1:
                    print(f"  Connection timeout. Device may not be in sync mode yet.")
                else:
                    print(f"  ❌ Connection timeout after {max_retries} attempts.")
            except Exception as e:
                if attempt < max_retries - 1:
                    print(f"  Connection error: {type(e).__name__}. Retrying...")
                else:
                    print(f"  ❌ An error occurred during monitoring: {type(e).__name__}")
        
        print(f"  Session ended for {self.name}.")
        self._update_status("OFF")

    async def check_device(self):
        pass