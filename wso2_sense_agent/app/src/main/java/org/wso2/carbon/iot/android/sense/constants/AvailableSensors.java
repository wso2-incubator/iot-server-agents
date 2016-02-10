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
package org.wso2.carbon.iot.android.sense.constants;

import android.hardware.Sensor;

import java.util.ArrayList;
import java.util.List;

/**
 * class to store the supported sensorDataMap types.
 */
public class AvailableSensors {

    public static final int SUPPORTED_SENSOR_COUNT = 10;
    private static List<String> sensorList = new ArrayList<>();

    //List of supported sensors by the system
    public static List<String> getList() {
        sensorList.add("Accelerometer");
        sensorList.add("Magnetometer");
        sensorList.add("Gravity");
        sensorList.add("Rotation Vector");
        sensorList.add("Pressure");
        sensorList.add("Light");
        sensorList.add("Gyroscope");
        sensorList.add("Proximity");
        return sensorList;
    }


    //Get the int type of the sensorDataMap
    public static int getType(String sensor) {
        int type = 1;
        switch (sensor) {
            case "accelerometer":
                type = Sensor.TYPE_ACCELEROMETER;
                break;
            case "magnetometer":
                type = Sensor.TYPE_MAGNETIC_FIELD;
                break;
            case "gravity":
                type = Sensor.TYPE_GRAVITY;
                break;
            case "rotation vector":
                type = Sensor.TYPE_ROTATION_VECTOR;
                break;
            case "pressure":
                type = Sensor.TYPE_PRESSURE;
                break;
            case "gyroscope":
                type = Sensor.TYPE_GYROSCOPE;
                break;
            case "light":
                type = Sensor.TYPE_LIGHT;
                break;
            case "proximity":
                type = Sensor.TYPE_PROXIMITY;

        }
        return type;
    }

    //Get the string type of te sensorDataMap
    public static String getType(int type) {
        String s = "";
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                s = "accelerometer";
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                s = "magnetometer";
                break;
            case Sensor.TYPE_GRAVITY:
                s = "gravity";
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                s = "rotation vector";
                break;
            case Sensor.TYPE_PRESSURE:
                s = "pressure";
                break;
            case Sensor.TYPE_GYROSCOPE:
                s = "gyroscope";
                break;
            case Sensor.TYPE_LIGHT:
                s = "light";
                break;
            case Sensor.TYPE_PROXIMITY:
                s = "proximity";

        }
        return s;
    }

}
