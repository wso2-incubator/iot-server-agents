/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.wso2.carbon.iot.android.sense.data.publisher;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.iot.android.sense.data.publisher.mqtt.AndroidSenseMQTTHandler;
import org.wso2.carbon.iot.android.sense.data.publisher.mqtt.transport.MQTTTransportHandler;
import org.wso2.carbon.iot.android.sense.data.publisher.mqtt.transport.TransportHandlerException;
import org.wso2.carbon.iot.android.sense.event.streams.Location.LocationData;
import org.wso2.carbon.iot.android.sense.event.streams.Sensor.SensorData;
import org.wso2.carbon.iot.android.sense.event.streams.battery.BatteryData;
import org.wso2.carbon.iot.android.sense.speech.detector.util.ProcessWords;
import org.wso2.carbon.iot.android.sense.speech.detector.util.WordData;
import org.wso2.carbon.iot.android.sense.util.SenseDataHolder;
import org.wso2.carbon.iot.android.sense.util.LocalRegistry;
//import org.wso2.carbon.iot.android.sense.util.SenseClient;

import java.util.List;

/**
 * This is an android service which publishes the data to the server.
 */
public class DataPublisherService extends Service {
    private static final String TAG = "Data Publisher";
    private static String KEY_TAG = "key";
    private static String TIME_TAG = "time";
    private static String VALUE_TAG = "value";
    public static Context context;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        Log.d(TAG, "service started");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {

                    JSONArray sensorJsonArray = new JSONArray();

                    //retreive sensor data.
                    List<SensorData> sensorDataMap = SenseDataHolder.getSensorDataHolder();
                    for (SensorData sensorData : sensorDataMap) {
                        JSONObject sensorJsonObject = new JSONObject();
                        sensorJsonObject.put(TIME_TAG, sensorData.getTimestamp());
                        sensorJsonObject.put(KEY_TAG, sensorData.getSensorType());
                        sensorJsonObject.put(VALUE_TAG, sensorData.getSensorValues());
                        sensorJsonArray.put(sensorJsonObject);
                    }
                    SenseDataHolder.resetSensorDataHolder();

                    //retreive batter data.
                    List<BatteryData> batteryDataMap = SenseDataHolder.getBatteryDataHolder();
                    for (BatteryData batteryData : batteryDataMap) {
                        JSONObject batteryJsonObject = new JSONObject();
                        batteryJsonObject.put(TIME_TAG, batteryData.getTimestamp());
                        batteryJsonObject.put(KEY_TAG, "battery");
                        batteryJsonObject.put(VALUE_TAG, batteryData.getLevel());
                        sensorJsonArray.put(batteryJsonObject);
                    }
                    SenseDataHolder.resetBatteryDataHolder();
                    //retreive location data.
                    List<LocationData> locationDataMap = SenseDataHolder.getLocationDataHolder();
                    for (LocationData locationData : locationDataMap) {
                        JSONObject locationJsonObject = new JSONObject();
                        locationJsonObject.put(TIME_TAG, locationData.getTimeStamp());
                        locationJsonObject.put(KEY_TAG, "GPS");
                        locationJsonObject.put(VALUE_TAG, locationData.getLatitude() + "," + locationData.getLongitude());
                        sensorJsonArray.put(locationJsonObject);
                    }
                    SenseDataHolder.resetLocationDataHolder();

                    //retreive words
                    ProcessWords.cleanAndPushToWordMap();
                    List<WordData> wordDatMap = SenseDataHolder.getWordDataHolder();
                    for (WordData wordData : wordDatMap) {
                        if(wordData.getOccurences() == 0) {
                            continue;
                        }
                        JSONObject wordJsonObject = new JSONObject();
                        wordJsonObject.put(TIME_TAG, System.currentTimeMillis());
                        wordJsonObject.put(KEY_TAG, "word");
                        String wordValue = wordData.getSessionId() + "," + wordData.getWord() + "," + wordData
                                .getOccurences() + "," + wordData.getTimestamps();
                        wordJsonObject.put(VALUE_TAG, wordValue);
                        sensorJsonArray.put(wordJsonObject);
                    }
                    SenseDataHolder.resetWordDataHolder();

                    //publish the data
                    if (sensorJsonArray.length() > 0) {
                        JSONObject jsonMsgObject = new JSONObject();
                        String user = LocalRegistry.getUsername(context);
                        String deviceId = LocalRegistry.getDeviceId(context);
                        jsonMsgObject.put("owner", user);
                        jsonMsgObject.put("deviceId", deviceId);
                        jsonMsgObject.put("values", sensorJsonArray);
                        MQTTTransportHandler mqttTransportHandler = AndroidSenseMQTTHandler.getInstance(context);
                        if(!mqttTransportHandler.isConnected()) {
                            mqttTransportHandler.connect();
                        }
                        mqttTransportHandler.publishDeviceData(user, deviceId, jsonMsgObject.toString());
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Json Data Parsing Exception", e);
                } catch (TransportHandlerException e) {
                    Log.e(TAG, "Data Publish Failed", e);
                }
            }
        };
        Thread dataUploaderThread = new Thread(runnable);
        dataUploaderThread.start();
        return Service.START_NOT_STICKY;
    }
}