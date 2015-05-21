#include "FireAlarmAgent.h"

#include <Ethernet.h>
#include <SPI.h>
#include "dht.h"

EthernetClient httpClient;
String host, jsonPayLoad;

int digitalPins[] = { TEMP_PIN, BULB_PIN, FAN_PIN };
int analogPins[] = { 0, 1, 2, 3, 4, 5 };

void setup() {
  Serial.begin(9600);
  connectHttp();
  setupResource();
}

void loop() {
  if (httpClient.connected()) {
    pushDigitalPinData();
    readControls();

  } else {
    Serial.println("client not found...");
    Serial.println("disconnecting.");
    httpClient.stop();
    connectHttp();

  }  
  
  delay(POLL_INTERVAL);

}


