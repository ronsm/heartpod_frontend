#!/bin/bash
# OpenHAB Migration Script: Pi 1 → Pi 2
# This script backs up Pi 1 and transfers to Pi 2

set -e

PI1_IP="192.168.2.198"
PI2_IP="192.168.2.150"
SSH_PASS="openhabian"
BACKUP_FILE="openhab_backup_$(date +%Y%m%d_%H%M%S).tar.gz"

echo "🔄 Step 1: Creating backup on Pi 1 ($PI1_IP)..."
ssh -o StrictHostKeyChecking=no openhabian@$PI1_IP << 'ENDSSH'
sudo tar -czf /tmp/openhab_backup.tar.gz \
    /etc/openhab \
    /var/lib/openhab \
    2>&1 | grep -v "socket ignored"
echo "✅ Backup created at /tmp/openhab_backup.tar.gz"
ENDSSH

echo "📥 Step 2: Downloading backup from Pi 1..."
scp -o StrictHostKeyChecking=no openhabian@$PI1_IP:/tmp/openhab_backup.tar.gz ./$BACKUP_FILE

echo "📤 Step 3: Uploading backup to Pi 2 ($PI2_IP)..."
scp -o StrictHostKeyChecking=no ./$BACKUP_FILE openhabian@$PI2_IP:/tmp/openhab_backup.tar.gz

echo "🔍 Step 4: Checking OpenHAB status on Pi 2..."
ssh -o StrictHostKeyChecking=no openhabian@$PI2_IP << 'ENDSSH'
if systemctl is-active --quiet openhab; then
    echo "⚠️  OpenHAB is running on Pi 2. Stopping..."
    sudo systemctl stop openhab
else
    echo "ℹ️  OpenHAB is not running on Pi 2."
fi

if ! command -v openhab-cli &> /dev/null; then
    echo "⚠️  OpenHAB not installed on Pi 2. Please install it first."
    exit 1
fi
ENDSSH

echo "📦 Step 5: Extracting backup on Pi 2..."
ssh -o StrictHostKeyChecking=no openhabian@$PI2_IP << 'ENDSSH'
sudo tar -xzf /tmp/openhab_backup.tar.gz -C / 2>&1 | grep -v "socket ignored"
sudo chown -R openhab:openhab /etc/openhab /var/lib/openhab
echo "✅ Configuration extracted and permissions set"
ENDSSH

echo "🚀 Step 6: Starting OpenHAB on Pi 2..."
ssh -o StrictHostKeyChecking=no openhabian@$PI2_IP << 'ENDSSH'
sudo systemctl start openhab
sudo systemctl enable openhab
sleep 5
systemctl status openhab --no-pager
ENDSSH

echo ""
echo "✅ Migration Complete!"
echo ""
echo "Next steps:"
echo "1. Update Android app IP (192.168.2.198 → 192.168.2.150)"
echo "2. Rebuild APK"
echo "3. Test connection"
echo ""
echo "Backup file saved: $BACKUP_FILE"
