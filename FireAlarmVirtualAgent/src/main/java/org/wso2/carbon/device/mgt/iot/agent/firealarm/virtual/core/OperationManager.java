/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.core;

import org.wso2.carbon.device.mgt.iot.agent.firealarm.virtual.ui.AgentUI;

import javax.swing.*;

public class OperationManager {

    private static OperationManager operationManager = new OperationManager();
    private AgentUI agentUI;
    private int temperature, humidity, interval;
    private int temperatureMin, temperatureMax, humidityMin, humidityMax;
    private boolean isTemperatureRandomized, isHumidityRandomized;
    private String deviceMgtControlUrl, deviceMgtAnalyticUrl;

    private OperationManager() {

    }

    public static OperationManager getInstance() {
        return operationManager;
    }

    public void init() {
        try {
            // Set System L&F
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
            // handle exception
        } catch (ClassNotFoundException e) {
            // handle exception
        } catch (InstantiationException e) {
            // handle exception
        } catch (IllegalAccessException e) {
            // handle exception
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                agentUI = new AgentUI();
                agentUI.setVisible(true);
            }
        });
    }

    public void changeBulbStatus(boolean isOn) {
        agentUI.setBulbStatus(isOn);
    }

    public void updateAgentStatus(String status) {
        agentUI.setAgentStatus(status);
    }

    public int getTemperature() {
        if (isTemperatureRandomized) {
            temperature = getRandom(temperatureMax, temperatureMin);
        }
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        if (isHumidityRandomized) {
            humidity = getRandom(humidityMax, humidityMin);
        }
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setTemperatureMin(int temperatureMin) {
        this.temperatureMin = temperatureMin;
    }

    public void setTemperatureMax(int temperatureMax) {
        this.temperatureMax = temperatureMax;
    }

    public void setHumidityMin(int humidityMin) {
        this.humidityMin = humidityMin;
    }

    public void setHumidityMax(int humidityMax) {
        this.humidityMax = humidityMax;
    }

    public void setIsHumidityRandomized(boolean isHumidityRandomized) {
        this.isHumidityRandomized = isHumidityRandomized;
    }

    public void setIsTemperatureRandomized(boolean isTemperatureRandomized) {
        this.isTemperatureRandomized = isTemperatureRandomized;
    }

    public String getDeviceMgtControlUrl() {
        return deviceMgtControlUrl;
    }

    public void setDeviceMgtControlUrl(String deviceMgtControlUrl) {
        this.deviceMgtControlUrl = deviceMgtControlUrl;
    }

    public String getDeviceMgtAnalyticUrl() {
        return deviceMgtAnalyticUrl;
    }

    public void setDeviceMgtAnalyticUrl(String deviceMgtAnalyticUrl) {
        this.deviceMgtAnalyticUrl = deviceMgtAnalyticUrl;
    }

    private int getRandom(int max, int min) {
        double rnd = Math.random() * (max - min) + min;
        return (int) Math.round(rnd);
    }
}
