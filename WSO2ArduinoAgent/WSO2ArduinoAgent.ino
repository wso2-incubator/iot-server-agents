#include <Ethernet.h>
#include <SPI.h>

#define DEVICE_IP "192.168.1.218"
#define SERVICE_IP "192.168.1.216"
#define SERVICE_PORT 9763 
#define SERVICE_EPOINT "/WSO2ConnectedDevices_1.0.0/services/connected_device/pushdata/"  //ip/owner/type/mac/pin/value
#define DEVICE_OWNER "smean"
#define DEVICE_TYPE "arduino-uno"
#define PUBLISH_INTERVAL 30000

enum IP_TYPE{
  SERVER,
  DEVICE
};

enum PIN_STATE{
  low,
  high
};

byte mac[] = { 0x90, 0xA2, 0xDA, 0x0D, 0x30, 0xD7 };  //mac - 90a2da0d30d7
byte dns2[] = { 8, 8, 8, 8 };
byte subnet[] = { 255, 255, 255, 0 };
byte gateway[] = { 192, 168, 1, 1 };

byte deviceIP[4] = { 0, 0, 0, 0 };
byte server[4] = { 0, 0, 0, 0 };

int pins[] = { 2, 5, 8, 9, 12 };

EthernetClient client;
String connecting = "connecting.... ";
String resource, tempResource, host;

void setup()
{  
  for ( int pin = 0; pin < (sizeof(pins)/sizeof(int)); pin++) {
    pinMode(pins[pin], OUTPUT);
  }

  Serial.begin(9600);
  Serial.println("-------------------------------");
  setIP(SERVICE_IP, SERVER);
  setIP(DEVICE_IP, DEVICE);
  Ethernet.begin(mac, deviceIP, dns2, gateway, subnet);
  
  Serial.print("My IP: ");
  Serial.println(Ethernet.localIP());
  connecting += client.connect(server, SERVICE_PORT);
  delay(3000);
  Serial.println(connecting);
  
  String port = String(SERVICE_PORT);
  host = String(SERVICE_IP);
  host = "Host: " + host + ":" + port;    //  Serial.println(host);
  
  resource = String(SERVICE_EPOINT) + String(SERVICE_IP) + "/" +  String(DEVICE_OWNER) + "/" + String(DEVICE_TYPE) + "/";
  
  for ( int i = 0; i < sizeof(mac); i++ ) {  
    if( mac[i] < 16){
      resource = resource + "0" + String(mac[i], HEX);
    }else{
      resource += String(mac[i], HEX);
    }
//    Serial.print(mac[i]);  Serial.print(" - ");   Serial.print(mac[i], HEX);  Serial.println();   
  }

  if (client.connected()) {
    Serial.println("connected");
  } else {
    Serial.println("connection failed");
  }
  
}

void loop()
{  
  alternatePinValue();
  
  if (client.connected()) {
    pushAnalogPinData();
    pushDigitalPinData(); 
  } else {
    Serial.println("client not found...");
    Serial.println("disconnecting.");
    client.stop();
    for(;;)
      ;
  }  
//  delay(PUBLISH_INTERVAL);
}


void pushDigitalPinData(){
  for ( int pin = 0; pin < (sizeof(pins)/sizeof(int)); pin++) {
    Serial.println("-------------------------------");
    tempResource = resource + "/D" + pins[pin] + "/";
    
    if (digitalRead(pins[pin]) == low) {
      tempResource += "LOW";
    } else {
      tempResource += "HIGH";
    }
   
    tempResource = "POST " + tempResource + " HTTP/1.1";    //Serial.println(tempResource);

    client.println(tempResource);
    client.println(host);
    client.println();
    delay(2000);
    
    while (client.available()) {
      char response = client.read();
      Serial.print(response);
    }
    
    Serial.println();
    tempResource = "";
    delay(4000);
  }
}

void pushAnalogPinData(){
  for ( int analogChannel = 0; analogChannel < 6; analogChannel++ ) {
    Serial.println("-------------------------------");
    tempResource = resource + "/A" + analogChannel + "/" + analogRead(analogChannel);
    tempResource = "POST " + tempResource + " HTTP/1.1";    Serial.println(tempResource);

    client.println(tempResource);
    client.println(host);
    client.println();
    delay(2000);
    
    while (client.available()) {
      char response = client.read();
      Serial.print(response);
    }
    
    Serial.println();
    tempResource = "";
    delay(4000);
  }
}

void alternatePinValue(){
  for ( int pin = 0; pin < (sizeof(pins)/sizeof(int)); pin++) {
    if(digitalRead(pins[pin]) == low) {
      digitalWrite(pins[pin], HIGH);
    } else {
      digitalWrite(pins[pin], LOW);
    }
  }
}

void setIP(String ip, int type){
  int dot3 = ip.lastIndexOf( "." );
  String ip_4 = ip.substring( dot3 + 1, ip.length() );   
  
  int dot2 = ip.lastIndexOf( "." , dot3 - 1 );
  String ip_3 = ip.substring( dot2 + 1, dot3 );   
  
  int dot1 = ip.indexOf( "." );
  String ip_2 = ip.substring( dot1 + 1, dot2 );     
  String ip_1 = ip.substring( 0, dot1 );   
  
  switch(type){
    case SERVER:
      server[3] = ip_4.toInt();    //      Serial.println(server[3]);
      server[2] = ip_3.toInt();    //      Serial.println(server[2]);
      server[1] = ip_2.toInt();    //      Serial.println(server[1]);
      server[0] = ip_1.toInt();    //      Serial.println(server[0]);
      break;
      
    case DEVICE:
      deviceIP[3] = ip_4.toInt();  //      Serial.println(deviceIP[3]);
      deviceIP[2] = ip_3.toInt();  //      Serial.println(deviceIP[2]);
      deviceIP[1] = ip_2.toInt();  //      Serial.println(deviceIP[1]);
      deviceIP[0] = ip_1.toInt();  //      Serial.println(deviceIP[0]);
      break;
  }
}
