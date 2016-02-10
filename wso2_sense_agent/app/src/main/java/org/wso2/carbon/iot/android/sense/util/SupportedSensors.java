/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.iot.android.sense.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import org.wso2.carbon.iot.android.sense.constants.AvailableSensors;
import org.wso2.carbon.iot.android.sense.constants.SenseConstants;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupportedSensors {

    private SharedPreferences sensorPreference;
    private SensorManager mSensorManager;

    public SupportedSensors(Context context) {
        this.sensorPreference = context.getSharedPreferences(SenseConstants.AVAILABLE_SENSORS, 0);
        this.mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void setContent() {
        List<String> sensor_List = AvailableSensors.getList();
        Set<String> sensorSet = new HashSet<>();
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        for (String sen : sensor_List) {
            if (sensors.contains(mSensorManager.getDefaultSensor(AvailableSensors.getType(sen.toLowerCase())))) {
                sensorSet.add(sen);
            }
        }

        SharedPreferences.Editor editor = this.sensorPreference.edit();
        editor.putStringSet(SenseConstants.GET_AVAILABLE_SENSORS, sensorSet);
        editor.apply();
    }


}
