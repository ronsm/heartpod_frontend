import asyncio
from bleak import BleakClient

# Replace with your device's MAC address
#DEVICE_ADDRESS = "CB:31:33:32:1F:8F" #OXIMETER 
#DEVICE_ADDRESS = "00:24:E4:AD:C8:17" #THERMO
#DEVICE_ADDRESS = "A0:9E:1A:E3:63:A1" #POLAR
#DEVICE_ADDRESS = "F0:A1:62:ED:E6:A9" OMRON BPM
DEVICE_ADDRESS = "A4:C1:38:BD:10:DB" #OxiPro BMP

async def explore_gatt():
    print(f"Connecting to device at {DEVICE_ADDRESS}...")
    try:
        # Use a context manager to ensure connection and disconnection
        async with BleakClient(DEVICE_ADDRESS) as client:
            print("Connected!")
            
            # The .services property is automatically populated after connection
            services = client.services 
            
            print("Discovering services...")
            for service in services:
                print(f"  Service: {service.uuid} ({service.description})")
                
                for char in service.characteristics:
                    print(f"    Characteristic: {char.uuid} (Handle: {char.handle})")
                    print(f"      Properties: {','.join(char.properties)}")
                    
                    if "read" in char.properties:
                        try:
                            value = await client.read_gatt_char(char.uuid)
                            print(f"      Value (bytes): {value}")
                        except Exception as e:
                            print(f"      Could not read value: {e}")

    except Exception as e:
        print(f"Could not connect or discover services: {e}")
        
async def main():
    await explore_gatt()

if __name__ == "__main__":
    asyncio.run(main())

#OXIMETER
#Connecting to device at CB:31:33:32:1F:8F...
#Connected!
#Discovering services...
#  Service: 00001801-0000-1000-8000-00805f9b34fb (Generic Attribute Profile)
#    Characteristic: 00002a05-0000-1000-8000-00805f9b34fb (Handle: 11)
#      Properties: read,indicate
#      Value (bytes): bytearray(b'\x01\x00\xff\xff')
#  Service: 0000ffe0-0000-1000-8000-00805f9b34fb (Vendor specific)
#    Characteristic: 0000ffe1-0000-1000-8000-00805f9b34fb (Handle: 15)
#      Properties: read,write-without-response,write,notify
#      Value (bytes): bytearray(b'\x01\x03\x04\x05\x06\x07\x08\t\n\x0b')
#    Characteristic: 0000ffe2-0000-1000-8000-00805f9b34fb (Handle: 19)
#      Properties: read,write-without-response,write,notify
#      Value (bytes): bytearray(b'\x01\x03\x04\x05\x06\x07\x08\t\n\x0b\x0c\r\x0e\x0f\x10\x11\x12\x13\x14\x15')
