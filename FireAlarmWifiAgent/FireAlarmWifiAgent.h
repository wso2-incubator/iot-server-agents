#ifndef FireAlarmWifiAgent_H
#define FireAlarmWifiAgent_H

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

#define LISTEN_PORT           80      // What TCP port to listen on for connections.  
                                      // The HTTP protocol uses port 80 by default.

#define MAX_ACTION            6      // Maximum length of the HTTP action that can be parsed.

#define MAX_PATH              10      // Maximum length of the HTTP request path that can be parsed.
                                      // There isn't much memory available so keep this short!

#define BUFFER_SIZE           MAX_ACTION + MAX_PATH + 10  // Size of buffer for incoming request data.
                                                          // Since only the first line is parsed this
                                                          // needs to be as large as the maximum action
                                                          // and path plus a little for whitespace and
                                                          // HTTP version.

#define TIMEOUT_MS            500    // Amount of time in milliseconds to wait for
                                     // an incoming request to finish.  Don't set this
                                     // too high or your server could be slow to respond.


#define WLAN_SSID       "WiFi-SSID" // "ShabirMeans-MAC"          // cannot be longer than 32 characters!
#define WLAN_PASS       "WiFi-PASSWORD"

byte server[4] = { 10, 100, 7, 38 };
byte mac[6] = { 0xb8, 0x27, 0xeb, 0x88, 0x37, 0x7a };

#define WLAN_SECURITY   WLAN_SEC_WPA2
                           // Security can be WLAN_SEC_UNSEC, WLAN_SEC_WEP, WLAN_SEC_WPA or WLAN_SEC_WPA2
//#define IDLE_TIMEOUT_MS  3000      

#define DEVICE_OWNER "${DEVICE_OWNER}"
#define DEVICE_ID "${DEVICE_ID}"
#define DEVICE_TOKEN "${DEVICE_TOKEN}"

#define SERVICE_PORT 9763 
#define SERVICE_EPOINT "/firealarm/controller/" 

#define TEMP_PIN A5
#define BULB_PIN A4
#define FAN_PIN A3
#define DEBUG false
#define CON_DEBUG true

static unsigned long pushTimestamp = 0;
String host, jsonPayLoad;
String responseMsg, subStrn;
uint32_t ipAddress;

#define PUSH_INTERVAL 2000


#endif

