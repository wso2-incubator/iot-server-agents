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
package org.wso2.carbon.iot.android.sense.sensordataview.realtimesensor;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import org.wso2.carbon.iot.android.sense.sensordataview.availablesensor.AvailableSensors;
import org.wso2.carbon.iot.android.sense.sensordataview.view.SensorViewAdaptor;

/**
 * Put data in to a map
 */
public class RealTimeSensorReader implements SensorEventListener {

    private Context context;
    private SensorViewAdaptor adaptor;
    private AvailableSensors availableSensors = AvailableSensors.getInstance();

    public RealTimeSensorReader(Context context, SensorViewAdaptor adaptor) {
        this.context = context;
        this.adaptor = adaptor;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        RealTimeSensor realTimeSensor = new RealTimeSensor();
        realTimeSensor.setName(availableSensors.getType(event.sensor.getType()).toUpperCase());

        realTimeSensor.setValueX(event.values[0] + "");
        realTimeSensor.setValueY(event.values[1] + "");
        realTimeSensor.setValueZ(event.values[2] + "");

        TempStore.sensorDataMap.put(availableSensors.getType(event.sensor.getType()), realTimeSensor);

        Intent intent = new Intent();
        intent.setAction("sensorDataMap");
        context.sendBroadcast(intent);

        adaptor.notifyDataSetChanged();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
