#include <Wire.h>
#include <ArduinoBLE.h>

const int GSR=A0;
int sensorValue=0;
int gsr_average=0;

BLEService anxietyService("1337D");

// create characteristics and allow remote device to read and get notifications:
BLEIntCharacteristic gsr("1A11", BLERead | BLENotify);
BLEIntCharacteristic heartrate("1A12", BLERead | BLENotify);

void setup(){
  Serial.begin(115200);
  while (!Serial);
  
  Wire.begin();

  while (!BLE.begin()) {
    Serial.println("Waiting for BLE to start");
    delay(1);
  }
  Serial.println("BLE start");

  // set the local name that the peripheral advertises:
  BLE.setLocalName("AnxioMeter");
  // set the UUID for the service:
  BLE.setAdvertisedService(anxietyService);

  // add the characteristics to the service
  anxietyService.addCharacteristic(gsr);
  anxietyService.addCharacteristic(heartrate);

  BLE.addService(anxietyService);

  // start advertising the service:
  BLE.advertise();
}

void loop(){
  BLEDevice central = BLE.central();

  if (central) {
    updateGSR();
    updateHeartrate();
  } 
  delay(500);
}

void updateGSR() {
  long sum=0;
  for(int i=0;i<10;i++)           //Average the 10 measurements to remove the glitch
  {
    sensorValue = analogRead(GSR);
    sum += sensorValue;
    delay(5);
  }
  gsr_average = sum/10;

  Serial.print("SR: "); // print it
  Serial.println(gsr_average);
  gsr.writeValue(gsr_average); 
}

void updateHeartrate() {
  Wire.requestFrom(0xA0 >> 1, 1);    // request 1 bytes from slave device
  while(Wire.available()) {          // slave may send less than requested
     int rate = (int) Wire.read();   // receive heart rate value (a byte)
     Serial.print("HR: "); // print it
     Serial.println(rate);
     heartrate.writeValue(rate); 
  }
}
