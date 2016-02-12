
package org.wso2.carbon.iot.android.sense.events.input.Sensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import org.wso2.carbon.iot.android.sense.constants.SenseConstants;
import org.wso2.carbon.iot.android.sense.events.input.DataReader;
import org.wso2.carbon.iot.android.sense.sensordataview.availablesensor.AvailableSensors;
import org.wso2.carbon.iot.android.sense.util.DataMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

public class SensorDataReader extends DataReader implements SensorEventListener {
    private SensorManager mSensorManager;
    private Map<String, SensorData> senseDataStruct = new HashMap<>();
    private Vector<SensorData> sensorVector = new Vector<>();
    Context ctx;
    private List<Sensor> sensorList1 = new ArrayList<>();
    private AvailableSensors availableSensors = AvailableSensors.getInstance();

    public SensorDataReader(Context context) {
        ctx=context;

        SharedPreferences sharedPreferences = ctx.getSharedPreferences(SenseConstants.SELECTED_SENSORS, Context.MODE_MULTI_PROCESS);
        Set<String> selectedSet = sharedPreferences.getStringSet(SenseConstants.SELECTED_SENSORS_BY_USER, null);
        mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);

        selectedSensorList(selectedSet);

        System.out.println(sensorList1.size());

        for(Sensor s : sensorList1){
            mSensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    private void collectSensorData(){

        for (Sensor sensor : sensorList1)
        {
            try{
                if (senseDataStruct.containsKey(sensor.getName())){

                    SensorData sensorInfo=senseDataStruct.get(sensor.getName());
                    sensorVector.add(sensorInfo);
                    Log.d(this.getClass().getName(),"Sensor Name "+sensor.getName()+", Type "+ sensor.getType() +  " " +
                            ", sensorValue :" + sensorInfo.getSensorValues());
                }
            }catch(Throwable e){
                Log.d(this.getClass().getName(),"error on sensors");
            }

        }
        mSensorManager.unregisterListener(this);
    }

    public Vector<SensorData> getSensorData(){
        try {
            TimeUnit.MILLISECONDS.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(SensorDataReader.class.getName(),e.getMessage());
        }
        collectSensorData();
        return sensorVector;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        senseDataStruct.put(event.sensor.getName(), new SensorData(event));
    }

    @Override
    public void run() {
        Log.d(this.getClass().getName(), "running -sensorDataMap");
        Vector<SensorData> sensorDatas=getSensorData();
        for( SensorData data : sensorDatas){
            DataMap.getSensorDataMap().add(data);
        }
    }

    public void selectedSensorList(Set<String> set){

        String[] fromsensorset = set.toArray(new String[set.size()]);

        for(String s : fromsensorset){
            sensorList1.add(mSensorManager.getDefaultSensor(availableSensors.getType(s.toLowerCase())));
        }
    }

}
