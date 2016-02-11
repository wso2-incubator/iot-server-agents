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


public class SenseConstants {
    public final static String DEVICE_TYPE = "android_sense";


    public final static String LOGIN_CONTEXT = "/devicemgt/api/user/authenticate";
    public final static String REGISTER_CONTEXT = "/android_sense_mgt/manager/device";
    public final static String DATA_ENDPOINT = "/android_sense/controller/sensordata";
    public final static String TRUSTSTORE_PASSWORD = "wso2carbon";


    //For set user selected sensors. Will be used by sensorDataMap reading and dialog
    public static String SELECTED_SENSORS = "Selected";
    public static String SELECTED_SENSORS_BY_USER = "userSelection";

    //For setting the available sensors in the device in dialog and SupportedSensors
    public static String AVAILABLE_SENSORS = "Sensors";
    public static String GET_AVAILABLE_SENSORS = "getAvailableSensors";



    public final class Request {
        public final static String REQUEST_SUCCESSFUL = "200";
        public final static String REQUEST_CONFLICT = "409";
        public final static int MAX_ATTEMPTS = 2;
    }
}
