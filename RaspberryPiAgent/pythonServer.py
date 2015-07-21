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

import sys, commands, time
import BaseHTTPServer
import RPi.GPIO as GPIO

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       HOST and PORT info of the HTTP Server that gets started
#			HOST_NAME is initialised in the main() method
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
global HOST_NAME
HOST_NAME = "0.0.0.0"

SERVER_PORT = 80 # Maybe set this to 9000.
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


global LAST_TEMP		
LAST_TEMP = 25 				# The Last read temperature value from the DHT sensor. Kept globally
							# Updated by the temperature reading thread

BULB_PIN = 11                                       # The GPIO Pin# in RPi to which the LED is connected


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Class that handles HTTP GET requests for operations on the RPi
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
class MyHandler(BaseHTTPServer.BaseHTTPRequestHandler):
	def do_GET(request):
		# """Respond to a GET request."""
		global LAST_TEMP

		if not processURLPath(request.path):
			return			

		resource = request.path.split("/")[1].upper()
		state = request.path.split("/")[2].upper()
		print "Resource: " + resource 

		if resource == "TEMP":
			request.send_response(200)
			request.send_header("Content-type", "text/plain")
			request.end_headers()
			request.wfile.write(LAST_TEMP)
			# return 

		elif resource == "BULB":
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
#       Check the URL string of the request and validate
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def processURLPath(path):
	if path.count("/") != 2 and not "favicon" in path:
		print "Invalid URL String: " + path
		return False
	
	resource = path.split("/")[1]

	if not iequal("BULB", resource) and not iequal("TEMP", resource) and not iequal("FAN", resource):
		if not "favicon" in resource:
			print "Invalid resource: " + resource + " to execute operation"
		return False

	return True
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       Case-Insensitive check on whether two string are similar
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def iequal(a, b):
	try:
		return a.upper() == b.upper()
	except AttributeError:
		return a == b
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
		pass

	GPIO.setup(BULB_PIN, GPIO.OUT)
	GPIO.output(BULB_PIN, False)
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#       The Main method of the server script
#			This method is invoked from RaspberryStats.py on a new thread
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
def main():
	global HOST_NAME

	setUpGPIOPins()

	HOST_NAME = getDeviceIP()

	server_class = BaseHTTPServer.HTTPServer
	
    	while True:
        	try:
           		httpd = server_class((HOST_NAME, SERVER_PORT), MyHandler)
            		print time.asctime(), "Server Starts - %s:%s" % (HOST_NAME, SERVER_PORT)

            		httpd.serve_forever()
        	except (KeyboardInterrupt, Exception) as e:
            		print "Exception in ServerThread (either KeyboardInterrupt or Other):"
            		print str(e)
            
   		 	GPIO.output(BULB_PIN, False)
            		httpd.server_close()
            		print time.asctime(), "Server Stops - %s:%s" % (HOST_NAME, SERVER_PORT)
            		print '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
            		pass

# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~



if __name__ == '__main__':
	main()
	
