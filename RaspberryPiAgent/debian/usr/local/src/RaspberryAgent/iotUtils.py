#!/usr/bin/env python

"""
/**
* Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
**/
"""

import time, commands
import RPi.GPIO as GPIO
#import Adafruit_DHT             # Adafruit library required for temperature sensing
import ConfigParser 

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#			HOST_NAME(IP) of the Device
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
global HOST_NAME
HOST_NAME = "0.0.0.0"
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


global LAST_TEMP		
LAST_TEMP = 25 				# The Last read temperature value from the DHT sensor. Kept globally
							# Updated by the temperature reading thread

BULB_PIN = 11               # The GPIO Pin# in RPi to which the LED is connected



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Device specific info when pushing data to server
#       Read from a file "deviceConfigs.cfg" in the same folder level
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
configParser = ConfigParser.RawConfigParser()
configFilePath = r'./deviceConfigs.cfg'
configParser.read(configFilePath)

DEVICE_OWNER = configParser.get('Device-Configurations', 'owner')
DEVICE_ID = configParser.get('Device-Configurations', 'deviceId')
MQTT_EP = configParser.get('Device-Configurations', 'mqtt-ep')
XMPP_EP = configParser.get('Device-Configurations', 'xmpp-ep')
AUTH_TOKEN = configParser.get('Device-Configurations', 'auth-token')

DEVICE_INFO = '{"owner":"'+ DEVICE_OWNER + '","deviceId":"' + DEVICE_ID  + '","reply":'
DEVICE_IP = '"{ip}","value":'
DEVICE_DATA = '"{temperature}"'                                                                      # '"{temperature}:{load}:OFF"'
### ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Method used to switch ON/OFF the LED attached to RPi
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def switchBulb(state):
    print "Requested Switch State: " + state

    if state == "ON":
        GPIO.output(BULB_PIN, True)
        print "BULB Switched ON"
    elif state == "OFF":
        GPIO.output(BULB_PIN, False)
        print "BULB Switched OFF"

    print '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Get the wlan0 interface via which the RPi is connected 
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def getDeviceIP():
    rPi_IP = commands.getoutput("ip route list | grep 'src '").split()
    rPi_IP = rPi_IP[rPi_IP.index('src') + 1]

    if len(rPi_IP)<=16:
		print "------------------------------------------------------------------------------------"
		print "IP Address of RaspberryPi: " + rPi_IP
		print "------------------------------------------------------------------------------------"
		return rPi_IP
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Set the GPIO pin modes for the ones to be read
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def setUpGPIOPins():
	try:
    		GPIO.setmode(GPIO.BOARD)
	except Exception as e:
	        print "Exception at 'GPIO.setmode'"
		pass

	GPIO.setup(BULB_PIN, GPIO.OUT)
	GPIO.output(BULB_PIN, False)
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       This method get the CPU Temperature of the Raspberry Pi
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def getCPUTemp():
    	CPU_TEMP_LOC = "/sys/class/thermal/thermal_zone0/temp"                                 # RaspberryPi file location to get CPU TEMP info
        tempFile = open(CPU_TEMP_LOC)
        cpuTemp = tempFile.read()
        cpuTemp = long(float(cpuTemp))
        cpuTemp = cpuTemp * 1.0 / 1000.0
        print "The CPU temperature is: %.2f" % cpuTemp
        return cpuTemp
### ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       This method get the CPU Load of the Raspberry Pi
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def getCPULoad():
    	CPU_LOAD_LOC = "/proc/loadavg"                                                          # RaspberryPi file location to get CPU LOAD info
        loadFile = open(CPU_LOAD_LOC)
        cpuLoad = loadFile.read()
        cpuLoad = cpuLoad.split()[0]
        cpuLoad = long(float(cpuLoad))
        print "The CPU temperature is: %.2f" % cpuLoad
        return cpuLoad
### ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
##       This is a Thread object for reading temperature continuously
## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#class TemperatureReaderThread(object):
#    def __init__(self, interval=3):
#        self.interval = interval
#        
#        thread = threading.Thread(target=self.run, args=())
#        thread.daemon = True                            # Daemonize thread
#        thread.start()                                  # Start the execution
#    
#    def run(self):
#        TEMP_PIN = 4
#        TEMP_SENSOR_TYPE = 11
#        global LAST_TEMP         
#
#        # Try to grab a sensor reading.  Use the read_retry method which will retry up
#        # to 15 times to get a sensor reading (waiting 2 seconds between each retry).
#        while True:
#            try:
#                humidity, temperature = Adafruit_DHT.read_retry(TEMP_SENSOR_TYPE, TEMP_PIN)
#                
#                if temperature != LAST_TEMP:
#                    LAST_TEMP = temperature
#                    print temperature
##                    connectAndPushData()
#
#                LAST_TEMP = temperature
#                
#                print 'Temp={0:0.1f}*C  Humidity={1:0.1f}%'.format(temperature, humidity)
#            
#            except Exception, e:
#                print "Exception in TempReaderThread: Could not successfully read Temperature"
#                print str(e)
#                print '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
#                pass
#    
#                time.sleep(self.interval)
## ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~






# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       The Main method of the server script
#			This method is invoked from RaspberryStats.py on a new thread
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def main():
#    setUpGPIOPins()
#
#    global HOST_NAME
#    HOST_NAME = getDeviceIP()
#    
#    TemperatureReaderThread()

    while True:
        pass

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

HOST_NAME = getDeviceIP()

if __name__ == '__main__':
	main()
	
