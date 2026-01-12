# main.py
import asyncio
from datetime import datetime
import config

from openhab.openhab_client import ensure_item_exists
from devices.polar_h10 import PolarH10
from devices.pulse_oximeter import PulseOximeter
from devices.omron_bp import OmronBP
from bleak import BleakScanner

def setup_open_hab_items():
    """Builds the full semantic model in OpenHAB based on the config."""
    print("--- ⚙️ Verifying OpenHAB Semantic Model ---")
    
    # 1. Create the top-level Health group
    print("Checking top-level group...")
    ensure_item_exists(
        item_name=config.HEALTH_GROUP["name"],
        item_type=config.HEALTH_GROUP["type"],
        item_label=config.HEALTH_GROUP["label"],
        base_url=config.OPENHAB_URL
    )
    
    # 2. Loop through devices to create Equipment groups and their member Items
    for device_key, device_config in config.DEVICES_CONFIG.items():
        equipment_name = list(device_config["items"].values())[0]["name"].split('_')[0]
        equipment_info = config.EQUIPMENT_DEFINITIONS[device_key]
        
        print(f"\nChecking Equipment group for {device_key}...")
        ensure_item_exists(
            item_name=equipment_name,
            item_type="Group",
            item_label=equipment_info["label"],
            base_url=config.OPENHAB_URL,
            tags=["Equipment", equipment_info["type"]],
            groups=[config.HEALTH_GROUP["name"]]
        )
        
        # 3. Create all Points and Properties for this Equipment
        for item_key, item_details in device_config.get("items", {}).items():
            ensure_item_exists(
                item_name=item_details["name"],
                item_type=item_details["type"],
                item_label=item_details["label"],
                base_url=config.OPENHAB_URL,
                tags=item_details.get("tags", []),
                groups=[equipment_name]
            )

async def main():
    """Main loop to find and monitor a single device."""
    print("--- 🩺 Health Device Monitor for OpenHAB Started ---")
    
    setup_open_hab_items()
    
    # Create a dictionary of our configured device objects, keyed by address
    monitored_devices = {
        config.DEVICES_CONFIG["POLAR_H10"]["address"]: PolarH10(config.DEVICES_CONFIG["POLAR_H10"], config.OPENHAB_URL),
        config.DEVICES_CONFIG["PULSE_OXIMETER"]["address"]: PulseOximeter(config.DEVICES_CONFIG["PULSE_OXIMETER"], config.OPENHAB_URL),
        config.DEVICES_CONFIG["OMRON_BP"]["address"]: OmronBP(config.DEVICES_CONFIG["OMRON_BP"], config.OPENHAB_URL),
    }
    
    # --- NEW MAIN LOOP ---
    while True:
        print(f"\n--- [{datetime.now().strftime('%H:%M:%S')}] Starting scan for available devices... ---")
        
        try:
            found_device = await BleakScanner.find_device_by_filter(
                lambda d, ad: d.address.upper() in monitored_devices,
                timeout=5.0
            )
        except Exception as e:
            print(f"  An error occurred during scanning: {e}")
            found_device = None

        if found_device:
            print(f"--- ✅ Device found: {found_device.name} ({found_device.address}) ---")
            device_object = monitored_devices[found_device.address.upper()]
            
            await device_object.monitor_device(found_device)
            
            print(f"--- Device disconnected. Returning to scanning mode. ---")
        
        else:
            print("  No devices found. Will scan again shortly.")
            for device in monitored_devices.values():
                device._update_status("OFF")
            
            await asyncio.sleep(config.CHECK_INTERVAL)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nProgram stopped by user.")
    finally:
        print("--- Monitor shutting down. ---")