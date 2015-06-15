boolean readControls() {
//  String responseMsg;
  
  pollClient.fastrprint(F("GET ")); 
  pollClient.fastrprint(SERVICE_EPOINT); pollClient.fastrprint(F("readcontrols/"));
  pollClient.fastrprint(DEVICE_OWNER); pollClient.fastrprint(F("/")); pollClient.fastrprint(DEVICE_ID);
  pollClient.fastrprint(F(" HTTP/1.1")); pollClient.fastrprint(F("\n"));
  pollClient.fastrprint(host.c_str()); pollClient.fastrprint(F("\n"));
  pollClient.println();

  delay(1000);
  
  
    while (pollClient.available()) {
      char response = pollClient.read();
      responseMsg += response;
    }
  
  
  
    int index = responseMsg.lastIndexOf(":");
    int newLine = responseMsg.lastIndexOf("\n");
    subStrn = responseMsg.substring(index + 1);
    responseMsg = responseMsg.substring(newLine + 1, index); 
    
  if(DEBUG) {
    Serial.print(responseMsg);
    Serial.println();
    Serial.println("-------------------------------");
  }
  
  
  if (subStrn.equals("IN")) {
    return false;
  }

  return true; 
}



