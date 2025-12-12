# Pi 2 OpenHAB Startup Issue - Quick Fix

## Problem
OpenHAB is installed on Pi 2 (192.168.2.150) but not running.

## Solution

### Step 1: Start OpenHAB Service

```bash
ssh openhabian@192.168.2.150
# Password: openhabian

sudo systemctl start openhab
sudo systemctl enable openhab

# Wait for startup
sleep 10

# Verify it's running
systemctl status openhab

# Exit
exit
```

### Step 2: Test Web Interface

Open in browser: `http://192.168.2.150:8080`

### Step 3: If Still Not Working

Check logs for errors:
```bash
ssh openhabian@192.168.2.150
sudo journalctl -u openhab -n 50 --no-pager
```

---

## After OpenHAB is Running

Once you confirm `http://192.168.2.150:8080` is accessible:

1. **Option A: Fresh Setup** - Configure OpenHAB from scratch on Pi 2
2. **Option B: Migrate from Pi 1** - Follow the migration steps to copy configuration from Pi 1

Let me know which option you prefer!
