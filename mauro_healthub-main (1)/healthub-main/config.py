# config.py

# -- OpenHAB Settings --
OPENHAB_URL = "http://127.0.0.1:8080"
# Add your copied API token here
OPENHAB_API_TOKEN = "oh.HEALTHUB.d8vf9C4nakvjOAaFH26j4k0rPGwDJBkkr63QDgzBj73CmandYbosr8yRP1FuGzjhuaaNIgM3qC6i9BM9KEYw" 
CHECK_INTERVAL = 5

# --- SEMANTIC MODEL DEFINITIONS ---

# 1. Top-Level Group for all health devices
HEALTH_GROUP = {"name": "gHealth", "label": "Health Monitoring", "type": "Group"}

# 2. Equipment definitions (linking a device to its semantic type)
EQUIPMENT_DEFINITIONS = {
    "POLAR_H10":      {"label": "Polar H10", "type": "HeartRateSensor"},
    "PULSE_OXIMETER": {"label": "Pulse Oximeter", "type": "PulseOximeter"},
    "OMRON_BP":       {"label": "Omron BP Monitor", "type": "BloodPressure"}
}

# 3. Item definitions with semantic tags and properties
DEVICES_CONFIG = {
    "POLAR_H10": {
        "address": "A0:9E:1A:E3:63:A1",
        "items": {
            # Points (Measurements)
            "heart_rate": {"name": "PolarH10_HeartRate", "label": "Heart Rate", "type": "Number:Frequency", "tags": ["Point", "Measurement"]},
            "battery":    {"name": "PolarH10_Battery",   "label": "Battery",    "type": "Number:Dimensionless", "tags": ["Point", "Measurement", "Level"]},
            # Properties (Associated Data)
            "status":     {"name": "PolarH10_Status",         "label": "Status",          "type": "Switch",   "tags": ["Property", "Status"]},
            "last_use":   {"name": "PolarH10_LastUse",        "label": "Last Use Time",   "type": "DateTime", "tags": ["Property", "Timestamp"]},
            "use_due":    {"name": "PolarH10_UseDue",         "label": "Use Due",         "type": "Switch",   "tags": ["Property", "Status"]},
            "video_url":  {"name": "PolarH10_InstructionsURL","label": "Instructions URL","type": "String",   "tags": ["Property"]},
            "picture_url": {"name": "PolarH10_PictureURL","label": "Picture URL","type": "String", "tags": ["Property"]}
        }
    },
    "PULSE_OXIMETER": {
        "address": "CB:31:33:32:1F:8F",
        "items": {
            "spo2":      {"name": "Oximeter_SpO2",   "label": "SpO2",            "type": "Number:Dimensionless", "tags": ["Point", "Measurement", "Level"]},
            "pulse":     {"name": "Oximeter_Pulse",  "label": "Pulse Rate",      "type": "Number:Frequency", "tags": ["Point", "Measurement"]},
            "status":    {"name": "Oximeter_Status", "label": "Status",          "type": "Switch",   "tags": ["Property", "Status"]},
            "last_use":  {"name": "Oximeter_LastUse","label": "Last Use Time",   "type": "DateTime", "tags": ["Property", "Timestamp"]},
            "use_due":   {"name": "Oximeter_UseDue", "label": "Use Due",         "type": "Switch",   "tags": ["Property", "Status"]},
            "video_url": {"name": "Oximeter_InstructionsURL","label": "Instructions URL","type": "String", "tags": ["Property"]},
            "picture_url": {"name": "Oximeter_PictureURL","label": "Picture URL","type": "String", "tags": ["Property"]}
        }
    },
    "OMRON_BP": {
        "address": "F0:A1:62:ED:E6:A9",
        "items": {
            "systolic":  {"name": "Omron_Systolic",  "label": "Systolic",        "type": "Number:Pressure", "tags": ["Point", "Measurement"]},
            "diastolic": {"name": "Omron_Diastolic", "label": "Diastolic",       "type": "Number:Pressure", "tags": ["Point", "Measurement"]},
            "status":    {"name": "Omron_Status",    "label": "Status",          "type": "Switch",   "tags": ["Property", "Status"]},
            "last_use":  {"name": "Omron_LastUse",   "label": "Last Use Time",   "type": "DateTime", "tags": ["Property", "Timestamp"]},
            "use_due":   {"name": "Omron_UseDue",    "label": "Use Due",         "type": "Switch",   "tags": ["Property", "Status"]},
            "video_url": {"name": "Omron_InstructionsURL","label": "Instructions URL","type": "String", "tags": ["Property"]},
            "picture_url": {"name": "Omron_PictureURL","label": "Picture URL","type": "String", "tags": ["Property"]}
        }
    }
}