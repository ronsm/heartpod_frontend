#!/bin/bash
# setup_wifi.sh
# Run this on the Raspberry Pi to configure Wi-Fi

if [ "$EUID" -ne 0 ]; then
  echo "Please run as root (sudo bash setup_wifi.sh)"
  exit
fi

if [ -n "$1" ] && [ -n "$2" ]; then
    SSID="$1"
    PSK="$2"
else
    echo "Scanning for available networks..."
    sudo iwlist wlan0 scan | grep ESSID
    echo ""
    read -p "Enter Wi-Fi SSID: " SSID
    read -s -p "Enter Wi-Fi Password: " PSK
    echo ""
fi

COUNTRY="US" # Change this if needed
WPA_FILE="/etc/wpa_supplicant/wpa_supplicant.conf"

# Backup existing config
cp $WPA_FILE "${WPA_FILE}.bak_$(date +%s)"
echo "Backed up existing config to ${WPA_FILE}.bak_$(date +%s)"

# Check if country exists, if not add it
if ! grep -q "country=" "$WPA_FILE"; then
    echo "country=$COUNTRY" >> "$WPA_FILE"
fi

# Append network block
# We use wpa_passphrase to generate the psk hash instead of cleartext
echo "Generating configuration..."
wpa_passphrase "$SSID" "$PSK" | tee -a "$WPA_FILE" > /dev/null

echo "Restarting network interface..."
wpa_cli -i wlan0 reconfigure

echo "✅ Wi-Fi configured. Checking IP address..."
sleep 5
hostname -I

echo "If you don't see an IP, try: sudo reboot"
