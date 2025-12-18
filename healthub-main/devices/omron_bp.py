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
        now = datetime.now()
        time_payload = struct.pack('<HBBBBBBBB', now.year, now.month, now.day, now.hour, now.minute, now.second, now.weekday() + 1, 0, 0)
        await client.write_gatt_char(CURRENT_TIME_CHAR_UUID, time_payload)
        await client.write_gatt_char(OMRON_WRITE_CHAR_UUID, b'\x01\x00', response=True)
        print("    Handshake complete. Awaiting data...")

    async def check_device(self):
        pass