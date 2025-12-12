# OpenHAB Migration: Step-by-Step Commands

## Step 1: Backup Pi 1 Configuration

Open a terminal and run these commands one by one:

```bash
# Connect to Pi 1
ssh openhabian@192.168.2.198
```

Once connected to Pi 1, run:
```bash
# Create backup (password: openhabian)
sudo tar -czf /tmp/openhab_backup.tar.gz /etc/openhab /var/lib/openhab

# Make it readable
sudo chmod 644 /tmp/openhab_backup.tar.gz

# Verify backup was created
ls -lh /tmp/openhab_backup.tar.gz

# Exit Pi 1
exit
```

---

## Step 2: Transfer Backup

From your local machine:
```bash
# Download from Pi 1 to local machine
scp openhabian@192.168.2.198:/tmp/openhab_backup.tar.gz ~/Downloads/

# Upload to Pi 2
scp ~/Downloads/openhab_backup.tar.gz openhabian@192.168.2.150:/tmp/
```

---

## Step 3: Restore on Pi 2

```bash
# Connect to Pi 2
ssh openhabian@192.168.2.150
```

Once connected to Pi 2, run:
```bash
# Stop OpenHAB if running
sudo systemctl stop openhab

# Extract backup
sudo tar -xzf /tmp/openhab_backup.tar.gz -C /

# Fix permissions
sudo chown -R openhab:openhab /etc/openhab /var/lib/openhab

# Start OpenHAB
sudo systemctl start openhab
sudo systemctl enable openhab

# Wait a few seconds
sleep 5

# Check status
systemctl status openhab

# Exit Pi 2
exit
```

---

## Step 4: Verify OpenHAB on Pi 2

Open a web browser and go to:
```
http://192.168.2.150:8080
```

You should see the OpenHAB dashboard with all your devices.

---

## Step 5: Update Android App

Once you confirm OpenHAB is working on Pi 2, type "ready" and I'll update the Android app to point to the new IP address (192.168.2.150).
