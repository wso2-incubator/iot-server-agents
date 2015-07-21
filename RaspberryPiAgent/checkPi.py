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

import Adafruit_DHT 
import RPi.GPIO as GPIO
import time

BULB_PIN = 11 
BUZZER_PIN = 12
TEMP_PIN = 4
TEMP_SENSOR_TYPE = 11

# Try to grab a sensor reading.  Use the read_retry method which will retry up
# to 15 times to get a sensor reading (waiting 2 seconds between each retry).
def readTemp():
    try:
        humidity, temperature = Adafruit_DHT.read_retry(TEMP_SENSOR_TYPE, TEMP_PIN)
        print 'Temp={0:0.1f}*C  Humidity={1:0.1f}%'.format(temperature, humidity)

    except Exception, e:
        print "Exception: Could not successfully read Temperature", e
        print '~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~'
        pass
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

GPIO.setmode(GPIO.BOARD)
GPIO.setup(BULB_PIN, GPIO.OUT)
GPIO.setup(BUZZER_PIN, GPIO.OUT)

GPIO.output(BULB_PIN, False)
GPIO.output(BUZZER_PIN, False)

while True:
	readTemp()
	GPIO.output(BULB_PIN, True)
	GPIO.output(BUZZER_PIN, True)
	time.sleep(2)

	GPIO.output(BULB_PIN, False)
	GPIO.output(BUZZER_PIN, False)
	time.sleep(2)
