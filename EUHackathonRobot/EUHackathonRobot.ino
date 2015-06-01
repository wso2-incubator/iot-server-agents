#include "EUHackathonRobot.h"

#include <Adafruit_CC3000.h>
#include <SPI.h>
#include "dht.h"
#include <pt.h>

Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                                         SPI_CLOCK_DIVIDER); // you can change this clock speed

Adafruit_CC3000_Client pushClient;

//static struct pt pushThread;

    /**********************************************************************************************  
        0. Check with a sample Wifi code of the Adafruit_CC3000 library to ensure that the sheild is working
        1. Set the ip of the server(byte array below) where the Web-Rest API for the FireAlarm is running
        2. Check whether the "SERVICE_EPOINT" is correct in the 'EUHackothonRobot.h' file
        3. Check whether the "SERVICE_PORT" is the same (9763) for the server running. Change it if needed
        4. Check whether the pins have been attached accordingly in the Arduino
        5. Check whether all reqquired pins are added to the 'digitalPins' array  
    ***********************************************************************************************/

uint32_t sserver;
byte server[4] = { 10, 100, 7, 38 };

String host, jsonPayLoad;

void setup() {
  if(true) Serial.begin(115200); 
  pinMode(PIR_PIN, INPUT);

//  PT_INIT(&pushThread);
  
  connectHttp();
  setupResource();
}

void loop() {
  
  // USE A DELAY with millis when using this code with motor controlling or simply use the thread block
  if (pushClient.connected()) {   
    pushData();                    // batches all the required pin values together and pushes once
//    protothread1(&pushThread, 1000);      // Pushes data and waits for control signals to be received
    delay(POLL_INTERVAL);
    
  } else {
    if(DEBUG) {
      Serial.println("client not found...");
      Serial.println("disconnecting.");
    }
    pushClient.close();
    cc3000.disconnect();  
   
    connectHttp();
  }  
}


//static int protothread1(struct pt *pt, int interval) {
//  static unsigned long timestamp = 0;
//  PT_BEGIN(pt);
//  while(1) { // never stop 
//    /* each time the function it is checked whether any control signals are sent
//    *  if so exit this proto thread and do the controlling part
//	  *  else continuously push sensor
//    */
////    PT_WAIT_UNTIL(pt, readControls() );
////   pushData();
//  }
//  PT_END(pt);
//}
