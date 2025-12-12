#!/bin/bash
# Run this script ON Pi 1 to create backup
echo "Creating OpenHAB backup..."
sudo tar -czf /tmp/openhab_backup.tar.gz \
    /etc/openhab \
    /var/lib/openhab \
    2>&1 | grep -v "socket ignored"

sudo chmod 644 /tmp/openhab_backup.tar.gz
echo "✅ Backup created at /tmp/openhab_backup.tar.gz"
ls -lh /tmp/openhab_backup.tar.gz
