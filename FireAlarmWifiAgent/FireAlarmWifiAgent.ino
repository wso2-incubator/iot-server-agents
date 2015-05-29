#include "FireAlarmWifiAgent.h"

#include <Adafruit_CC3000.h>
#include <SPI.h>
#include "dht.h"

int digitalPins[] = { TEMP_PIN, BULB_PIN, FAN_PIN };
int analogPins[] = { 0, 1, 2, 3, 4, 5 };

Adafruit_CC3000 cc3000 = Adafruit_CC3000(ADAFRUIT_CC3000_CS, ADAFRUIT_CC3000_IRQ, ADAFRUIT_CC3000_VBAT,
                                         SPI_CLOCK_DIVIDER); // you can change this clock speed

Adafruit_CC3000_Client httpClient;
String host, jsonPayLoad, replyMsg;

void setup() {
  if(DEBUG) Serial.begin(115200); 
  pinMode(BULB_PIN, OUTPUT);
  pinMode(FAN_PIN, OUTPUT);
  connectHttp();
  setupResource();
}

void loop() {
  if (httpClient.connected()) {    
    pushDigitalPinData();

    delay(POLL_INTERVAL);
    
    String responseMsg = readControls();
    int index = responseMsg.lastIndexOf(":");
    int newLine = responseMsg.lastIndexOf("\n");
    String subStrn = responseMsg.substring(index + 1);
    responseMsg = responseMsg.substring(newLine + 1, index); 
    
//    if (subStrn.equals("IN")) {
//      int temperature =  (uint8_t)getTemperature();
//      replyMsg = "Temperature is " + String(temperature) + " C";
//      reply();    
//    } else if (subStrn.equalsIgnoreCase("ON")) {
//      if (responseMsg.equals("BULB")) {
//        digitalWrite(BULB_PIN, HIGH);
//        replyMsg = "Bulb was switched ON";
//      } else if (responseMsg.equals("FAN")) {
//        digitalWrite(FAN_PIN, HIGH);
//        replyMsg = "Buzzer was switched ON";
//      }  
//    } else if (subStrn.equalsIgnoreCase("OFF")) {
//      if (responseMsg.equals("BULB")) {
//        digitalWrite(BULB_PIN, LOW);
//        replyMsg = "Bulb was switched OFF";  
//      } else if (responseMsg.equals("FAN")) {
//        digitalWrite(FAN_PIN, LOW);
//        replyMsg = "Buzzer was switched OFF";
//      }  
//    }


    if (subStrn.equals("IN")) { 
      if (responseMsg.equals("TEMPERATURE")) {
        int temperature =  (uint8_t)getTemperature();
        replyMsg = "Temperature is " + String(temperature) + " C";
        reply();
      } else if (responseMsg.equals("BULB")) {
        replyMsg = "Bulb was switched " + switchBulb();
      } else if (responseMsg.equals("FAN")) {
        replyMsg = "Buzzer was switched " + switchFan();
      }    
    } 
  } else {
    if(DEBUG) {
      Serial.println("client not found...");
      Serial.println("disconnecting.");
    }
    
    httpClient.close();
    connectHttp();

  }  
}




String switchBulb() {
    if (digitalRead(BULB_PIN) == HIGH) {
      digitalWrite(BULB_PIN, LOW);
      return "OFF";
    } else {
      digitalWrite(BULB_PIN, HIGH);
      return "ON";
    }
}

String switchFan() {
    if (digitalRead(FAN_PIN) == HIGH) {
      digitalWrite(FAN_PIN, LOW);
      return "OFF";
    } else {
      digitalWrite(FAN_PIN, HIGH);
      return "ON";
    }
}


double getTemperature(){
  dht DHT;
  
  if(DEBUG) {
    Serial.println("-------------------------------");
    Serial.println("Type,\tstatus,\tHumidity (%),\tTemperature (C)");
    Serial.print("DHT11, \t");
  }
  int chk = DHT.read11(TEMP_PIN);
  switch (chk)
  {
    case DHTLIB_OK:  
		if(DEBUG)  Serial.print("OK,\t"); 
		break;
    case DHTLIB_ERROR_CHECKSUM: 
		if(DEBUG)  Serial.print("Checksum error,\t"); 
		break;
    case DHTLIB_ERROR_TIMEOUT: 
		if(DEBUG)  Serial.print("Time out error,\t"); 
		break;
    case DHTLIB_ERROR_CONNECT:
                if(DEBUG)  Serial.print("Connect error,\t");
                break;
    case DHTLIB_ERROR_ACK_L:
                if(DEBUG) Serial.print("Ack Low error,\t");
                break;
    case DHTLIB_ERROR_ACK_H:
                if(DEBUG) Serial.print("Ack High error,\t");
                break;
    default: 
		if(DEBUG) Serial.print("Unknown error,\t"); 
		break;
  }
  
                        // DISPLAY DATA
  if(DEBUG) {
    Serial.print("\t");
    Serial.print(DHT.temperature, 1);
    Serial.print(",\t\t");
    Serial.println(DHT.humidity, 1);
    Serial.println("-------------------------------");
  }
  return DHT.temperature;
}
