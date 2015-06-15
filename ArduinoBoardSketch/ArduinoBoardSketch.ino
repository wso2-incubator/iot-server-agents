#include "Arduinoboardwifi.h"

#include <Adafruit_CC3000.h>
#include <SPI.h>
#include "dht.h"
#include <pt.h> 

Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                                         SPI_CLOCK_DIVIDER); // you can change this clock speed

Adafruit_CC3000_Client pushClient;
Adafruit_CC3000_Client pollClient;
static struct pt pushThread;

uint32_t sserver;

    /**********************************************************************************************  
        0. Check with a sample Wifi code of the Adafruit_CC3000 library to ensure that the sheild is working
        1. Set the ip of the server(byte array below) where the Web-Rest API for the FireAlarm is running
        2. Check whether the "SERVICE_EPOINT" is correct in the 'FireAlarmWifiAgent.h' file
        3. Check whether the "SERVICE_PORT" is the same (9763) for the server running. Change it if needed
        4. Check whether the pins have been attached accordingly in the Arduino
        5. Check whether all reqquired pins are added to the 'digitalPins' array  
    ***********************************************************************************************/

byte server[4] = { 10, 100, 7, 38 };
String host, jsonPayLoad, replyMsg;
String responseMsg, subStrn;

void setup()
{
  Serial.begin(9600);

  Serial.println(F("Internal Temperature Sensor"));
}

void loop()
{
  
  if (pushClient.connected() && pollClient.connected()) {   
    pushData();                    
    delay(POLL_INTERVAL);
    
    boolean valid = readControls();
    
    if (!valid) {
      if (responseMsg.equals("ON")) {
          setLED(true);
      
      } else if (responseMsg.equals("OFF")){
        setLED(false);
       
      
      }
    } 
  } else {
    if(DEBUG) {
      Serial.println("client not found...");
      Serial.println("disconnecting.");
    }
    pushClient.close();
    pollClient.close();
    cc3000.disconnect();  
   
    connectHttp();
  }  
  
  
 
   pinMode(13, OUTPUT);
  delay(1000);
}

void setLED(boolean status){
  if(status){
    digitalWrite(13, HIGH); 
  }else{
    digitalWrite(13, LOW); 
  }

}




double getBoardTemp(void)
{
  unsigned int wADC;
  double t;

  // The internal temperature has to be used
  // with the internal reference of 1.1V.
  // Channel 8 can not be selected with
  // the analogRead function yet.

  // Set the internal reference and mux.
  ADMUX = (_BV(REFS1) | _BV(REFS0) | _BV(MUX3));
  ADCSRA |= _BV(ADEN);  // enable the ADC

  delay(20);            // wait for voltages to become stable.

  ADCSRA |= _BV(ADSC);  // Start the ADC

  // Detect end-of-conversion
  while (bit_is_set(ADCSRA,ADSC));

  // Reading register "ADCW" takes care of how to read ADCL and ADCH.
  wADC = ADCW;

  // The offset of 324.31 could be wrong. It is just an indication.
  t = (wADC - 324.31 ) / 1.22;

  // The returned temperature is in degrees Celcius.
  return (t);
}

static int protothread1(struct pt *pt, int interval) {
  static unsigned long timestamp = 0;
  PT_BEGIN(pt);
  while(1) { // never stop 
    /* each time the function it is checked whether any control signals are sent
    *  if so exit this proto thread
    */
    PT_WAIT_UNTIL(pt, readControls() );
    pushData();
  }
  PT_END(pt);
}
