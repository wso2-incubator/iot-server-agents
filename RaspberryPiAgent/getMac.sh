#!/bin/sh

piMac=`/sbin/ifconfig | grep 'wlan0' | tr -s ' ' | cut -d ' ' -f5 |  tr -d ':'`
echo The device ID is $piMac