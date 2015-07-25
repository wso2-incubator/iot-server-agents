#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"""

import sleekxmpp
import getpass
import sys
import ssl, pyasn1

import iotUtils

#import logging
#from optparse import OptionParser
from urllib import urlopen


# Python versions before 3.0 do not use UTF-8 encoding
# by default. To ensure that Unicode is handled properly
# throughout SleekXMPP, we will set the default encoding
# ourselves to UTF-8.
if sys.version_info < (3, 0):
    from sleekxmpp.util.misc_ops import setdefaultencoding
    setdefaultencoding('utf8')
else:
    raw_input = input

from sleekxmpp.plugins.xep_0323.device import Device

class IoT_TestDevice(sleekxmpp.ClientXMPP):
    """
    A simple IoT device that can act as server or client
    """
    def __init__(self, jid, password):
        sleekxmpp.ClientXMPP.__init__(self, jid, password)
        self.add_event_handler("session_start", self.session_start)
        self.add_event_handler("message", self.message)
        self.device=None
        self.releaseMe=False
        self.beServer=True

    def beClientOrServer(self,server=True,clientJID=None ):
        self.beServer=True

    def testForRelease(self):
        # todo thread safe
        return self.releaseMe

    def doReleaseMe(self):
        # todo thread safe
        self.releaseMe=True

    def addDevice(self, device):
        self.device=device

    def session_start(self, event):
        self.send_presence()
        self.get_roster()
        # tell your preffered friend that you are alive
        #self.send_message(mto='jocke@jabber.sust.se', mbody=self.boundjid.bare +' is now online use xep_323 stanza to talk to me')

    def message(self, msg):
        if msg['type'] in ('chat', 'normal'):
            print ("got normal chat message" + str(msg))
            ip=urlopen('http://icanhazip.com').read()
            msg.reply("Hi I am " + self.boundjid.full + " and I am on IP " + ip).send()
        else:
            print ("got unknown message type %s", str(msg['type']))

class TheDevice(Device):
    """
    This is the actual device object that you will use to get information from your real hardware
    You will be called in the refresh method when someone is requesting information from you
    """
    def __init__(self,nodeId):
        Device.__init__(self,nodeId)

    def refresh(self,fields):
        """
        the implementation of the refresh method
        """
#        global LAST_TEMP
        #self._set_momentary_timestamp(self._get_timestamp())
        #self._add_field_momentary_data(self, "Temperature", self.counter)

        self._add_field(name="Temperature", typename="numeric", unit="C")
        self._set_momentary_timestamp(self._get_timestamp())
        self._add_field_momentary_data("Temperature", str(iotUtils.LAST_TEMP), flags={"automaticReadout": "true"})

def main():
    # Setup the command line arguments.
    #
    # This script can act both as
    #   "server" an IoT device that can provide sensorinformation
    #   python IoT_TestDevice.py -j "serverjid@yourdomain.com" -p "password" -n "TestIoT" --debug
    #
    #   "client" an IoT device or other party that would like to get data from another device

#    optp = OptionParser()
#
#    # Output verbosity options.
#    optp.add_option('-q', '--quiet', help='set logging to ERROR',
#                    action='store_const', dest='loglevel',
#                    const=logging.ERROR, default=logging.INFO)
#    optp.add_option('-d', '--debug', help='set logging to DEBUG',
#                    action='store_const', dest='loglevel',
#                    const=logging.DEBUG, default=logging.INFO)
#    optp.add_option('-v', '--verbose', help='set logging to COMM',
#                    action='store_const', dest='loglevel',
#                    const=5, default=logging.INFO)
#    optp.add_option('-t', '--pingto', help='set jid to ping',
#                    action='store', type='string', dest='pingjid',
#                    default=None)

    # JID and password options.
#    optp.add_option("-j", "--jid", dest="jid",
#                    help="JID to use")
#    optp.add_option("-p", "--password", dest="password",
#                    help="password to use")
#
#    # IoT test
#    optp.add_option("-n", "--nodeid", dest="nodeid",
#                    help="I am a device get ready to be called", default=None)
#
#    opts, args = optp.parse_args()
#
#    if opts.jid is None:
#        opts.jid = "1hrud08yceu01@192.168.57.129/raspi"
#        #opts.jid = raw_input("Username: ")
#    if opts.password is None:
#        opts.password = "wso2iot123"
#        #opts.password = getpass.getpass("Password: ")
#    if opts.nodeid is None:
#        opts.nodeid = "Device01"
#        #opts.nodeid = getpass.getpass("Node ID: ")

    XMPP_ENDP = iotUtils.XMPP_EP.split(":")[0]
   
    XMPP_OWN = iotUtils.DEVICE_OWNER
    XMPP_JID = iotUtils.DEVICE_ID + "@" + XMPP_ENDP + "/raspi" 	
    XMPP_PWD = iotUtils.AUTH_TOKEN

    xmpp = IoT_TestDevice(XMPP_JID,XMPP_PWD)

    xmpp.ssl_version = ssl.PROTOCOL_SSLv3

    xmpp.register_plugin('xep_0030')
    #xmpp['xep_0030'].add_feature(feature='urn:xmpp:iot:sensordata',
    #                             node=None,
    #    jid=None)
    xmpp.register_plugin('xep_0323')
    xmpp.register_plugin('xep_0325')

    if XMPP_OWN:
        # xmpp['xep_0030'].add_feature(feature='urn:xmpp:sn',
        # node=opts.nodeid,
        # jid=xmpp.boundjid.full)

        myDevice = TheDevice(XMPP_OWN)
        # myDevice._add_field(name="Relay", typename="numeric", unit="Bool");
        myDevice._add_field(name="Temperature", typename="numeric", unit="C")
        myDevice._set_momentary_timestamp("2013-03-07T16:24:30")
        myDevice._add_field_momentary_data("Temperature", "23.4", flags={"automaticReadout": "true"})

        xmpp['xep_0323'].register_node(nodeId=XMPP_OWN, device=myDevice, commTimeout=10)
        xmpp.beClientOrServer(server=True)
        while not(xmpp.testForRelease()):
	    try:
            	xmpp.connect()
            	xmpp.process(block=True)
            	print ("lost connection")
	    except Exception as e:
		print "Exception in XMPPServerThread (either KeyboardInterrupt or Other):"
		print str(e)

if __name__ == '__main__':
    main()

