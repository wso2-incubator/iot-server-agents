package org.wso2.carbon.iot.android.sense.transport.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.wso2.carbon.iot.android.sense.constants.SenseConstants;
import org.wso2.carbon.iot.android.sense.transport.TransportHandlerException;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

public class AndroidSenseMQttTransportHandler extends MQTTTransportHandler {


    private static String iotServerSubscriber = UUID.randomUUID().toString().substring(0, 5);

//    private static final String DEVICE_TYPE = "android_sense";

    private static AndroidSenseMQttTransportHandler androidSenseMQttTransportHandler;

    private static String publishTopic = SenseConstants.MQTT_TRANSPORT_TOPIC;

    protected AndroidSenseMQttTransportHandler() {
        super(iotServerSubscriber, SenseConstants.DEVICE_TYPE, "tcp://localhost:1883", "");
    }

    private ScheduledFuture<?> dataPushServiceHandler;

    public ScheduledFuture<?> getDataPushServiceHandler() {
        return dataPushServiceHandler;
    }

    @Override
    public void connect() {
        Runnable connect = new Runnable() {
            @Override
            public void run() {

                while (!isConnected()) {
                    try {
                        connectToQueue();
                    } catch (TransportHandlerException e) {
                        Log.w("ASMQttTransportHandler", "Connection" + mqttBrokerEndPoint + " failed");

                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException ex) {
                            Log.e("ASMQttTransportHandler", "MQTT-Subscriber: Thread Sleep Interrupt Exception");
                        }
                    }
                }

                Log.i("ASMQttTransportHandler", "Connected..");

            }
        };

        Thread connectorThread = new Thread(connect);
        connectorThread.setDaemon(true);
        connectorThread.start();

    }

    @Override
    public void processIncomingMessage(MqttMessage message, String... messageParams) {
    }

    public void publishToAndroidSense(String owner, String deviceId, String payLoad, int qos, boolean retained)
            throws TransportHandlerException{
        String topic = String.format(publishTopic,owner,deviceId);
        publishToQueue(topic, payLoad, qos, retained);
    }

    @Override
    public void disconnect() {
        Runnable stopConnection = new Runnable() {
            public void run() {
                while (isConnected()) {
                    try {
                        dataPushServiceHandler.cancel(true);
                        closeConnection();

                    } catch (MqttException e) {
                            Log.w("ASMQttTransportHandler", "Unable to 'STOP' MQTT connection at broker at:" +
                                    " " +
                                    mqttBrokerEndPoint);


                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException e1) {
                            Log.e("ASMQttTransportHandler", "MQTT-Terminator: Thread Sleep Interrupt Exception");
                        }
                    }
                }
            }
        };

        Thread terminatorThread = new Thread(stopConnection);
        terminatorThread.setDaemon(true);
        terminatorThread.start();
    }


    @Override
    public void publishDeviceData() throws TransportHandlerException {

    }

    @Override
    public void publishDeviceData(MqttMessage publishData) throws TransportHandlerException {

    }

    @Override
    public void publishDeviceData(String... publishData) throws TransportHandlerException {

    }

    @Override
    public void processIncomingMessage() {

    }

    @Override
    public void processIncomingMessage(MqttMessage message) throws TransportHandlerException {

    }

    public static AndroidSenseMQttTransportHandler getInstance(){
        if(androidSenseMQttTransportHandler == null){
            androidSenseMQttTransportHandler = new AndroidSenseMQttTransportHandler();
            androidSenseMQttTransportHandler.connect();
        }
        return androidSenseMQttTransportHandler;
    }

}