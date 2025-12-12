#!/bin/bash
# Run this script ON Pi 2 to restore backup
echo "Stopping OpenHAB (if running)..."
sudo systemctl stop openhab 2>/dev/null || true

echo "Extracting backup..."
sudo tar -xzf /tmp/openhab_backup.tar.gz -C / 2>&1 | grep -v "socket ignored"

echo "Setting permissions..."
sudo chown -R openhab:openhab /etc/openhab /var/lib/openhab

echo "Starting OpenHAB..."
sudo systemctl start openhab
sudo systemctl enable openhab

sleep 5
echo "✅ OpenHAB status:"
systemctl status openhab --no-pager | head -20
