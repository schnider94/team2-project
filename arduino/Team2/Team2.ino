#include <CurieBLE.h>

BLEPeripheral blePeripheral; // Our board

// Skin Conductance Sensor BLE vars
BLEService skinConductanceAndHeartRate("1337D"); // Service for skin conductance
BLECharacteristic SkinCondChar("1A11", BLERead | BLENotify, 2);

// Variables for skin conductance
int oldConductance = 0;  // last heart rate reading from analog input
int prevMs = 0;
int packetRateMs = 200; // check packs every 200 ms.
int ledPin = 13;

// Skin Conductance Sensor BLE vars
BLECharacteristic heartRateChar("1A12", BLERead | BLENotify, 2);

// Variables for heart rate.
int prevBeatRate = 0;

void updateSkinConductance();
void updateHeartRate();

void setup()
{
	Serial.begin(9600);
	pinMode(ledPin, OUTPUT);   // Pin 13 LED
	blePeripheral.setLocalName("SkinConductanceAndHeartRate"); // Local name in advertising packets.
	blePeripheral.setAdvertisedServiceUuid(skinConductanceAndHeartRate.uuid());  // Service UUID
	blePeripheral.addAttribute(skinConductanceAndHeartRate);
	// Skin Conductance sensor setup
	blePeripheral.addAttribute(SkinCondChar);
	// Skin Conductance sensor setup
	blePeripheral.addAttribute(heartRateChar);
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
				updateHeartRate();
			}
		}
		// when the central disconnects, turn off the LED:
		digitalWrite(ledPin, LOW);
		Serial.println("Central disconnected: " + central.address());
	}
}

void updateSkinConductance()
{
	int conductanceMeasurement = analogRead(A0); // Let's say skin conductance sensor is on A0.
	int conductance = map(conductanceMeasurement, 0, 1023, 0, 1000); // Let's say we measure between 0 to 1000 mV.
	if(conductance != oldConductance)
	{
		Serial.println("Skin conductance: " + conductance);
		// Update the characteristic.
		const unsigned char SkinCondCharA[2] = {0, (char)conductance};
		SkinCondChar.setValue(SkinCondCharA, 2);
		oldConductance = conductance;
	}
}

void updateHeartRate()
{
	int heartRateMeasurement = analogRead(A1); // Let's say heart rate sensor is on A1.
	int beatRate = map(heartRateMeasurement, 0, 1023, 0, 200); // Make sure to update!! these values correctly when obtaining actual sensor!_!!__!_!
	if(beatRate != prevBeatRate)
	{
		Serial.println("Heart Rate: " + beatRate);
		// Update the characteristic.
		const unsigned char heartCondCharA[2] = {0, (char)beatRate};
		SkinCondChar.setValue(heartCondCharA, 2);
		prevBeatRate = beatRate;
	}
}