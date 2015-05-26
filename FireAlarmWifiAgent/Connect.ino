byte mac[6] = { 0xC0, 0x4A, 0x00, 0x1A, 0x03, 0xF8 };  //mac - 90a2da0d30d7
//byte dns2[] = { 8, 8, 8, 8 };
//byte subnet[] = { 255, 255, 255, 0 };
//byte gateway[] = { 192, 168, 1, 1 };
//byte deviceIP[4] = { 192, 168, 1, 219 };
//byte server[4] = { 192, 168, 1, 216 };
byte server[4] = { 10, 100, 7, 38 };

//uint32_t ip, ddns, ssubnet, ggateway, sserver;
uint32_t sserver;
String connecting = "connecting.... ";

void connectHttp() {
  /* Initialise the module */
  if(true) Serial.println(F("\nInitializing..."));
  if (!cc3000.begin())
  {
    if(true) Serial.println(F("Couldn't begin()! Check your wiring?"));
    while(1);
  }
  
  if( cc3000.setMacAddress(mac) ) {
    uint8_t address[6];
    cc3000.getMacAddress(address);
    Serial.print(address[0], HEX); Serial.print(":");
    Serial.print(address[1], HEX); Serial.print(":");
    Serial.print(address[2], HEX); Serial.print(":");
    Serial.print(address[3], HEX); Serial.print(":");
    Serial.print(address[4], HEX); Serial.print(":");
    Serial.println(address[5], HEX); 
  }
  
//  ip = cc3000.IP2U32(deviceIP[0], deviceIP[1], deviceIP[2], deviceIP[3]);
//  ddns = cc3000.IP2U32(dns2[0], dns2[1], dns2[2], dns2[3]);
//  ssubnet = cc3000.IP2U32(subnet[0], subnet[1], subnet[2], subnet[3]);
//  ggateway = cc3000.IP2U32(gateway[0], gateway[1], gateway[2], gateway[3]);
  sserver = cc3000.IP2U32(server[0], server[1], server[2], server[3]);
  
//  cc3000.setStaticIPAddress(ip, ssubnet, ggateway, ddns);
  
  if(true) {
    Serial.print(F("\nAttempting to connect to ")); 
    Serial.println(WLAN_SSID);
  }
  
  if (!cc3000.connectToAP(WLAN_SSID, WLAN_PASS, WLAN_SECURITY)) {
    if(true) Serial.println(F("Failed!"));
    while(1);
  }
   
  if(true) Serial.println(F("Connected to Wifi network!"));
  
  Serial.println(F("Request DHCP"));
    while (!cc3000.checkDHCP())
  {
    delay(100); // ToDo: Insert a DHCP timeout!
  }  

  /* Display the IP address DNS, Gateway, etc. */  
  while (! displayConnectionDetails()) {
    delay(1000);
  }
  
  httpClient = cc3000.connectTCP(sserver, SERVICE_PORT);  //SERVICE_PORT
  if (httpClient.connected()) {
    if(true) Serial.println("Connected to server");
  } else {
    if(true) Serial.println(F("Connection failed"));    
    
    while(!httpClient.connected()){
      if(true) Serial.println("retrying to connect......");
      httpClient = cc3000.connectTCP(sserver, SERVICE_PORT);
      delay(1000);
    }
  }

  if(true) Serial.println(F("-------------------------------------"));
}


void setupResource(){ 
  String hostIP = getHostIP(server);
  String port = String(SERVICE_PORT);
  
  host = "Host: " + hostIP + ":" + port;      
  if(DEBUG) Serial.println(host);
  
  jsonPayLoad = "{\"owner\":\"";
  jsonPayLoad += String(DEVICE_OWNER);
  jsonPayLoad += "\",\"deviceId\":\"";
  jsonPayLoad += String(DEVICE_ID);
  jsonPayLoad += "\",\"replyMessage\":\"";

  if(DEBUG) {
    Serial.print("JSON Payload: ");
    Serial.println(jsonPayLoad);
    Serial.println("-------------------------------");
  }
}


String getMyIP(byte deviceIP[4]){
  String myIP = String(deviceIP[0]);
  
  for ( int index = 1; index < 4; index++) {
    myIP += "." + String(deviceIP[index]);
  }
  
  return myIP;
}


String getHostIP(byte server[4]){
  String hostIP = String(server[0]);
  
  for ( int index = 1; index < 4; index++) {
    hostIP += "." + String(server[index]);
  }
  
  return hostIP;
}


bool displayConnectionDetails(void)
{
  uint32_t ipAddress, netmask, gateway, dhcpserv, dnsserv;
  
  if(!cc3000.getIPAddress(&ipAddress, &netmask, &gateway, &dhcpserv, &dnsserv))
  {
    Serial.println(F("Unable to retrieve the IP Address!\r\n"));
    return false;
  }
  else
  {
    Serial.print(F("\nIP Addr: ")); cc3000.printIPdotsRev(ipAddress);
    Serial.print(F("\nNetmask: ")); cc3000.printIPdotsRev(netmask);
    Serial.print(F("\nGateway: ")); cc3000.printIPdotsRev(gateway);
    Serial.print(F("\nDHCPsrv: ")); cc3000.printIPdotsRev(dhcpserv);
    Serial.print(F("\nDNSserv: ")); cc3000.printIPdotsRev(dnsserv);
    Serial.println();
    return true;
  }
}
