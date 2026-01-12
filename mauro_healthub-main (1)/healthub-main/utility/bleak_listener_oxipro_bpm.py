import asyncio
from bleak import BleakScanner, BleakClient
import sys

DEVICE_ADDRESS = "A4:C1:38:BD:10:DB" 
CHECK_INTERVAL_SECONDS = 60

# --- UUIDs for the OxiPro BPM ---
NOTIFY_CHAR_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb"
WRITE_CHAR_UUID  = "0000ffe2-0000-1000-8000-00805f9b34fb" # The characteristic to send commands to

# --- The command to trigger a data transfer ---
# This is a guess. You may need to experiment with other values.
# See the guide below for other common commands to try.
START_COMMAND = b'\x02'

class OxiProManager:
    def __init__(self, address):
        self.address = address
        self.processed_packets = set()

    def _notification_handler(self, sender, data):
        """Handles, decodes, and filters incoming data packets."""
        packet_hex = data.hex()
        if packet_hex in self.processed_packets:
            return
        
        if packet_hex.startswith("d0c20ccc"):
            self.processed_packets.add(packet_hex)
            print("\n--- ✅ New Blood Pressure Reading Received ---")
            try:
                systolic = data[4]
                diastolic = data[5]
                pulse = data[6]
                year = int.from_bytes(data[8:10], byteorder="little")
                month, day, hour, minute, second = data[10:15]

                print(f"🩸 Systolic: {systolic} mmHg")
                print(f"🩸 Diastolic: {diastolic} mmHg")
                print(f"❤️ Pulse Rate: {pulse} bpm")
                print(f"🗓️ Timestamp: {year}-{month:02d}-{day:02d} {hour:02d}:{minute:02d}:{second:02d}")
                print("-" * 41)
            except IndexError:
                print(f"⚠️  Received a result packet but it was too short to parse: {packet_hex}")

    async def run_sync_session(self):
        """Runs a single connect-sync-disconnect session."""
        try:
            device = await BleakScanner.find_device_by_address(self.address, timeout=10.0)
            if not device:
                print("Device not found. Will try again later.")
                return

            async with BleakClient(device, timeout=20.0) as client:
                print(f"✅ Connected to {self.address} to check for new data...")
                
                await client.start_notify(NOTIFY_CHAR_UUID, self._notification_handler)
                
                # --- THIS IS THE NEW, CRUCIAL STEP ---
                print(f"Sending START_COMMAND (hex): {START_COMMAND.hex()} to trigger data sync...")
                await client.write_gatt_char(WRITE_CHAR_UUID, START_COMMAND)
                
                # Wait for records to be transmitted
                await asyncio.sleep(10)
                print("Check complete. Disconnecting...")

        except Exception as e:
            print(f"❌ An error occurred during the sync session: {e}")

async def main_loop(address):
    manager = OxiProManager(address)
    while True:
        # Take a new measurement BEFORE the script is scheduled to check
        await manager.run_sync_session()
        print(f"--- Waiting for {CHECK_INTERVAL_SECONDS} seconds until next check ---")
        await asyncio.sleep(CHECK_INTERVAL_SECONDS)

if __name__ == "__main__":
    try:
        asyncio.run(main_loop(DEVICE_ADDRESS))
    except KeyboardInterrupt:
        print("\nProgram stopped by user.")
