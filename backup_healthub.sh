#!/bin/bash
# Backup script for HealthHub Raspberry Pi
# Creates complete backup of SD card and configurations

BACKUP_DIR="/home/kwalker96/healthub-backups/$(date +%Y%m%d_%H%M%S)"
PI_IP="192.168.2.150"
PI_USER="openhabian"
PI_PASS="openhabian"

echo "🔄 Creating HealthHub Backup..."
echo "Backup location: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

# 1. Backup OpenHAB configuration
echo "📦 Backing up OpenHAB configuration..."
sshpass -p "$PI_PASS" scp -r -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:/etc/openhab/ \
    "$BACKUP_DIR/openhab_config/"

# 2. Backup OpenHAB data (items, persistence)
echo "📦 Backing up OpenHAB data..."
sshpass -p "$PI_PASS" scp -r -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:/var/lib/openhab/ \
    "$BACKUP_DIR/openhab_data/"

# 3. Backup BLE Monitor code
echo "📦 Backing up BLE Monitor..."
sshpass -p "$PI_PASS" scp -r -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:~/healthub-main/ \
    "$BACKUP_DIR/healthub-main/"

# 4. Backup LangGraph Agent
echo "📦 Backing up LangGraph Agent..."
sshpass -p "$PI_PASS" scp -r -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:~/langraph-backend/ \
    "$BACKUP_DIR/langraph-backend/"

# 5. Backup systemd services
echo "📦 Backing up systemd services..."
mkdir -p "$BACKUP_DIR/systemd"
sshpass -p "$PI_PASS" scp -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:/etc/systemd/system/healthub-*.service \
    "$BACKUP_DIR/systemd/"

# 6. Backup network configuration
echo "📦 Backing up network config..."
mkdir -p "$BACKUP_DIR/network"
sshpass -p "$PI_PASS" scp -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:/etc/wpa_supplicant/wpa_supplicant.conf \
    "$BACKUP_DIR/network/"
sshpass -p "$PI_PASS" scp -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:/etc/dhcpcd.conf \
    "$BACKUP_DIR/network/"

# 7. Backup environment files
echo "📦 Backing up environment files..."
sshpass -p "$PI_PASS" scp -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:~/langraph-backend/.env \
    "$BACKUP_DIR/langraph-backend/" 2>/dev/null || echo "No .env file found"
sshpass -p "$PI_PASS" scp -o StrictHostKeyChecking=no \
    ${PI_USER}@${PI_IP}:~/.bashrc \
    "$BACKUP_DIR/bashrc" 2>/dev/null

# 8. Export service status
echo "📦 Exporting service status..."
sshpass -p "$PI_PASS" ssh -o StrictHostKeyChecking=no ${PI_USER}@${PI_IP} \
    "systemctl status openhab mosquitto healthub-ble healthub-agent" \
    > "$BACKUP_DIR/service_status.txt"

# 9. Export OpenHAB items state
echo "📦 Exporting OpenHAB items..."
curl -s -H "Authorization: Bearer oh.NHSTHT.QViW3MMVzsp56R8PNt3maoKrv9Z7iP7LNRymiPG25bYqlOXgV0BgggwQ8ZCbBbBdPTy6WxbBW0u0BBqCkiG9w" \
    http://${PI_IP}:8080/rest/items > "$BACKUP_DIR/openhab_items.json"

# 10. Create backup metadata
echo "📦 Creating backup metadata..."
cat > "$BACKUP_DIR/BACKUP_INFO.txt" << EOF
HealthHub Backup
================
Date: $(date)
Pi IP: $PI_IP
Hostname: $(sshpass -p "$PI_PASS" ssh -o StrictHostKeyChecking=no ${PI_USER}@${PI_IP} hostname)
OS: $(sshpass -p "$PI_PASS" ssh -o StrictHostKeyChecking=no ${PI_USER}@${PI_IP} "cat /etc/os-release | grep PRETTY_NAME")

Services Backed Up:
- OpenHAB configuration
- OpenHAB data
- BLE Monitor code
- LangGraph Agent code
- Systemd services
- Network configuration
- Environment files

Restore Instructions:
1. Flash new SD card with OpenHAB image
2. Configure network (copy network/wpa_supplicant.conf)
3. Restore OpenHAB: sudo cp -r openhab_config/* /etc/openhab/
4. Restore data: sudo cp -r openhab_data/* /var/lib/openhab/
5. Copy code: scp -r healthub-main/ langraph-backend/ openhabian@192.168.2.150:~/
6. Copy services: sudo cp systemd/*.service /etc/systemd/system/
7. Reload systemd: sudo systemctl daemon-reload
8. Enable services: sudo systemctl enable healthub-ble healthub-agent
9. Start services: sudo systemctl start healthub-ble healthub-agent
EOF

# 11. Create compressed archive
echo "📦 Creating compressed archive..."
cd "$(dirname "$BACKUP_DIR")"
tar -czf "$(basename "$BACKUP_DIR").tar.gz" "$(basename "$BACKUP_DIR")"

echo "✅ Backup complete!"
echo "Location: $BACKUP_DIR"
echo "Archive: $BACKUP_DIR.tar.gz"
echo "Size: $(du -sh "$BACKUP_DIR" | cut -f1)"

# Optional: Create SD card image (requires Pi to be accessible)
read -p "Create full SD card image? (requires sudo, takes ~30min) [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📦 Creating SD card image..."
    echo "Note: This requires the Pi's SD card to be accessible via SSH or physically"
    echo "For physical backup, use: sudo dd if=/dev/mmcblk0 of=$BACKUP_DIR/sdcard.img bs=4M status=progress"
    echo "Skipping automatic SD image creation (requires physical access)"
fi

echo "🎉 Backup process complete!"
