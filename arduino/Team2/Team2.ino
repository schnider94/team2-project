#include <CurieBLE.h>

BLEPeripheral blePeripheral; // Our board
BLEService skinConductance("1337D"); // Service for skin conductance

// BLE Heart Rate Measurement Characteristic"
BLECharacteristic SkinCondChar("1A11", BLERead | BLENotify, 2);

int oldConductance = 0;  // last heart rate reading from analog input
int prevMs = 0;
int packetRateMs = 200; // check packs every 200 ms.
int ledPin = 13;

void updateSkinConductance();

void setup()
{
	Serial.begin(9600);
	pinMode(ledPin, OUTPUT);   // Pin 13 LED

	blePeripheral.setLocalName("SkinConductance"); // Local name in advertising packets.
	blePeripheral.setAdvertisedServiceUuid(skinConductance.uuid());  // Service UUID
	blePeripheral.addAttribute(skinConductance);
	blePeripheral.addAttribute(SkinCondChar);

	// Activate device, will send advertising packs continuously.
	blePeripheral.begin();
	Serial.println("Waiting for connections..");
}

void loop()
{
	BLECentral central = blePeripheral.central();

	if(central)
	{
		Serial.println("Central connected: " + Serial.println(central.address()));
		digitalWrite(ledPin, HIGH); // Let's show connection using the LED.

		while(central.connected())
		{
			long currentMs = millis();
			if(currentMs - prevMs >= packetRateMs)
			{
				prevMs = currentMs;
				updateSkinConductance();
			}
		}
		// when the central disconnects, turn off the LED:
		digitalWrite(ledPin, LOW);
		Serial.println("Central disconnected: " + central.address());
	}
}

void updateSkinConductance()
{
	int conductanceMeasurement = analogRead(A0); // Let's say sensor is on A0.
	int conductance = map(heartRateMeasurement, 0, 1023, 0, 1000); // Let's say we measure between 0 to 1000 mV.
	if(conductance != oldConductance)
	{
		Serial.println("Skin conductance: " + conductance);
		// Update the characteristic.
		const unsigned char SkinCondCharA[2] = {0, (char)conductance};
		SkinCondChar.setValue(heartRateCharArray, 2);
		oldConductance = conductance;
	}
}