# devices/base_device.py
import abc
import asyncio
import traceback # Import for detailed error logging
from datetime import datetime 
from openhab.openhab_client import update_openhab_item
from bleak import BleakClient

class Device(abc.ABC):
    """Abstract base class for a BLE health device."""

    def __init__(self, config, openhab_url):
        self.name = self.__class__.__name__
        self.address = config.get("address")
        self.items = config.get("items", {})
        self.openhab_url = openhab_url
        self.status_item = self.items.get("status", {}).get("name")
        self.last_known_status = None

    @abc.abstractmethod
    async def _start_monitoring_notifications(self, client):
        """Subclasses must implement this to start their specific notifications."""
        raise NotImplementedError

    async def monitor_device(self, ble_device):
        """Connects to a device and monitors it until it disconnects."""
        disconnected_event = asyncio.Event()

        def disconnected_callback(client):
            print(f"  ⚠️ Device {client.address} disconnected.")
            disconnected_event.set()

        print(f"  Attempting to connect to {self.name} at {ble_device.address}...")
        self._update_status("ON")
        try:
            # --- MODIFIED LINE: Added a longer timeout ---
            async with BleakClient(ble_device, disconnected_callback=disconnected_callback, timeout=20.0) as client:
                if client.is_connected:
                    print(f"  ✅ Connected! Now monitoring {self.name} for live data...")
                    await self._start_monitoring_notifications(client)
                    await disconnected_event.wait()
                else:
                    print(f"  ❌ Failed to connect to {self.name}.")
        except Exception as e:
            # --- MODIFIED BLOCK: Added detailed traceback printing ---
            print(f"  ❌ An error occurred during monitoring: An exception of type {type(e).__name__} occurred.")
            traceback.print_exc()
        finally:
            print(f"  Session ended for {self.name}.")
            self._update_status("OFF")

    def _update_status(self, new_status: str):
        """Helper to update the device's online status in OpenHAB, avoiding redundant updates."""
        if new_status == self.last_known_status:
            # print(f"  {self.name} status is already {new_status}. No update needed.")
            return

        print(f"  {self.name} status changed to {new_status}. Sending update to OpenHAB.")
        update_openhab_item(self.status_item, new_status, self.openhab_url)
        self.last_known_status = new_status

    def _update_data(self, data_dict: dict):
        """Helper to update multiple data points and the last_use timestamp in OpenHAB."""
        for key, value in data_dict.items():
            item_config = self.items.get(key)
            if item_config and "name" in item_config:
                update_openhab_item(item_config["name"], value, self.openhab_url)
        
        last_use_item = self.items.get("last_use", {}).get("name")
        if last_use_item:
            timestamp = datetime.now().isoformat()
            update_openhab_item(last_use_item, timestamp, self.openhab_url)