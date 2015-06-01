#ifndef EUHackathonRobot_H
#define EUHackathonRobot_H

#if (ARDUINO >= 100)
 #include "Arduino.h"
#else
 #include "WProgram.h"
#endif

// These are the interrupt and control pins
#define ADAFRUIT_CC3000_IRQ   3  // MUST be an interrupt pin!
// These can be any two pins
#define ADAFRUIT_CC3000_VBAT  5
#define ADAFRUIT_CC3000_CS    10

#define WLAN_SSID       "ssidName"           // cannot be longer than 32 characters!
#define WLAN_PASS       "Password"

#define WLAN_SECURITY   WLAN_SEC_WPA2
                           // Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
#define IDLE_TIMEOUT_MS  3000      

#define DEVICE_OWNER "${DEVICE_OWNER}"      // used by the template engine 
#define DEVICE_ID "${DEVICE_ID}"            //  when these args are auto      
 #define DEVICE_TOKEN "${DEVICE_TOKEN}"	    //    generated from web UI 



#define SERVICE_PORT 9763 
#define SERVICE_EPOINT "/iotdevices/SenseBotController/" 
                                        // pushalarmdata - application/json - {"owner":"","deviceId":"","replyMessage":"","time":"","key":"","value":""}
                                        // readcontrols/{owner}/{deviceId}
                                        // reply - application/json - {"owner":"","deviceId":"","replyMessage":""}

#define SONAR_TRIG  2
#define SONAR_ECHO  4
#define PIR_PIN  13
#define LDR_PIN  A1
#define TEMP_PIN 12

#define BUZZER 6
#define BUZZER_SOUND 100
#define MAX_DISTANCE 30

#define TURN_DELAY 100

#define POLL_INTERVAL 1000
#define DEBUG false
#define CON_DEBUG true

#endif


