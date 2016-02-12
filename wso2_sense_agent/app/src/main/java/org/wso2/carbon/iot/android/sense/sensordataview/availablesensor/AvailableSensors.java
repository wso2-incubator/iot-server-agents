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
package org.wso2.carbon.iot.android.sense.sensordataview.availablesensor;

import android.hardware.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * class to store the supported sensorDataMap types.
 */
public class AvailableSensors {

    public static final int SUPPORTED_SENSOR_COUNT = 10;
    private static List<String> sensorList = new ArrayList<>();
    private static HashMap<String, Integer> sensorTypeMap = new HashMap<>();
    private static HashMap<Integer, String> typeSensorMap = new HashMap<>();
    private static AvailableSensors availableSensors = new AvailableSensors();

    private AvailableSensors() {
        this.setList();
        this.setSensorTypeMap();
        this.setTypeSensorMap();
    }

    public static AvailableSensors getInstance() {
        return availableSensors;
    }
    //List of supported sensors by the system
    private void setList() {
        sensorList.add("Accelerometer");
        sensorList.add("Magnetometer");
        sensorList.add("Gravity");
        sensorList.add("Rotation Vector");
        sensorList.add("Pressure");
        sensorList.add("Light");
        sensorList.add("Gyroscope");
        sensorList.add("Proximity");
    }

    private void setSensorTypeMap() {
        sensorTypeMap.put("accelerometer", Sensor.TYPE_ACCELEROMETER);
        sensorTypeMap.put("magnetometer", Sensor.TYPE_MAGNETIC_FIELD);
        sensorTypeMap.put("gravity", Sensor.TYPE_GRAVITY);
        sensorTypeMap.put("rotation vector", Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorTypeMap.put("pressure", Sensor.TYPE_PRESSURE);
        sensorTypeMap.put("gyroscope", Sensor.TYPE_GYROSCOPE);
        sensorTypeMap.put("light", Sensor.TYPE_LIGHT);
        sensorTypeMap.put("proximity", Sensor.TYPE_PROXIMITY);
    }

    private void setTypeSensorMap() {
        typeSensorMap.put(Sensor.TYPE_ACCELEROMETER, "accelerometer");
        typeSensorMap.put(Sensor.TYPE_MAGNETIC_FIELD, "magnetometer");
        typeSensorMap.put(Sensor.TYPE_GRAVITY, "gravity");
        typeSensorMap.put(Sensor.TYPE_GAME_ROTATION_VECTOR, "rotation vector");
        typeSensorMap.put(Sensor.TYPE_PRESSURE, "pressure");
        typeSensorMap.put(Sensor.TYPE_GYROSCOPE, "gyroscope");
        typeSensorMap.put(Sensor.TYPE_LIGHT, "light");
        typeSensorMap.put(Sensor.TYPE_PROXIMITY, "proximity");
    }

    public List<String> getSensorList() {
        return sensorList;
    }

    //Get the int type of the sensorDataMap
    public int getType(String sensor) {
        return sensorTypeMap.get(sensor);
    }

    //Get the string type of te sensorDataMap
    public String getType(int type) {
        return typeSensorMap.get(type);
    }

}
