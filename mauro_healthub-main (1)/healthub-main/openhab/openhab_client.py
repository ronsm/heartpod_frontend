# openhab_client.py
import requests
import config

def _get_auth_headers():
    """Returns the authentication headers for OpenHAB API requests."""
    if not hasattr(config, 'OPENHAB_API_TOKEN') or not config.OPENHAB_API_TOKEN:
        return {}
    return {
        "Authorization": f"Bearer {config.OPENHAB_API_TOKEN}"
    }

def ensure_item_exists(item_name, item_type, item_label, base_url, tags=None, groups=None):
    """Checks if an OpenHAB item exists and creates it if not, using the universally compatible PUT method."""
    item_url = f"{base_url}/rest/items/{item_name}"
    headers = _get_auth_headers()
    
    try:
        response = requests.get(item_url, headers=headers, timeout=3)
        if response.status_code == 200:
            print(f"  ✅ Item '{item_name}' already exists.")
            return True
        
        if response.status_code == 404:
            print(f"  ⚠️ Item '{item_name}' not found. Creating it...")
            
            create_headers = {**headers, 'Content-Type': 'application/json'}
            create_payload = {
                "name": item_name,
                "type": item_type,
                "label": item_label,
                "tags": tags or [],
                "groupNames": groups or []
            }
            
            create_response = requests.put(item_url, json=create_payload, headers=create_headers, timeout=5)

            if create_response.status_code in [200, 201]:
                print(f"  ✅ Successfully created item '{item_name}'.")
                return True
            else:
                print(f"  ❌ Error creating item '{item_name}'. Status: {create_response.status_code}, Response: {create_response.text}")
                return False
                
    except requests.exceptions.RequestException as e:
        print(f"  ❌ Could not connect to OpenHAB at {base_url}. Error: {e}")
        return False

def update_openhab_item(item_name, value, base_url):
    """Sends a state update to a specific OpenHAB item."""
    if not base_url or not item_name:
        print("  ⚠️ OpenHAB URL or item name is not configured. Skipping update.")
        return

    url = f"{base_url}/rest/items/{item_name}/state"
    headers = {**_get_auth_headers(), 'Content-Type': 'text/plain', 'Accept': 'application/json'}

    try:
        response = requests.put(url, data=str(value), headers=headers, timeout=3)
        
        # --- THIS IS THE CORRECTED LINE ---
        # Added 202 to the list of valid success codes.
        if response.status_code in [200, 201, 202]:
            print(f"  ✅ Successfully updated OpenHAB item '{item_name}' to '{value}'")
        else:
            print(f"  ❌ Error updating '{item_name}'. Status: {response.status_code}, Response: {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"  ❌ Could not connect to OpenHAB at {base_url}. Error: {e}")