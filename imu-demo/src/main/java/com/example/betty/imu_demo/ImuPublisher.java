package com.example.betty.imu_demo;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeListener;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

import sensor_msgs.Imu;

/**
 * Created by root on 3/30/17.
 */
public class ImuPublisher implements NodeMain {

    private ImuThread imuThread;

    private SensorListener sensorListener;

    private SensorManager sensorManager;

    private Publisher<Imu> publisher;

    public ImuPublisher(SensorManager msensorManager) {
        this.sensorManager = msensorManager;
    }


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("android_sensors_driver/imuPublisher");
    }

    public interface NodeMain extends NodeListener {

        GraphName getDefaultNodeName();

    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        try
        {
            //主题为"android/imu"    消息类型为 "sensor_msgs/Imu" ,是标准类型的消息

            this.publisher = connectedNode.newPublisher("android/imu","sensor_msgs/Imu");

            int i = this.sensorManager.getSensorList(1).size();

            boolean hasAccel = false;

            if (i > 0)

                hasAccel = true;

            int j = this.sensorManager.getSensorList(4).size();

            boolean hasGyro = false;

            if (j > 0)

                hasGyro = true;

            int k = this.sensorManager.getSensorList(11).size();

            boolean hasQuat = false;

            if (k > 0)

                hasQuat = true;

            this.sensorListener = new SensorListener(publisher, hasAccel, hasGyro, hasQuat);


            this.imuThread = new ImuThread(this.sensorManager, sensorListener);

            this.imuThread.start();


        }catch (Exception e){

            if(connectedNode != null)

            {
                connectedNode.getLog().fatal(e);

            }else

            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onShutdown(Node node) {

        this.imuThread.shutdown();

        try
        {
            this.imuThread.join();

            return;
        }
        catch (InterruptedException localInterruptedException)
        {
            localInterruptedException.printStackTrace();
        }
    }

    @Override
    public void onShutdownComplete(Node node) {

    }

    @Override
    public void onError(Node node, Throwable throwable) {

    }

    /**
     * run(){}注册传感器监听事件
     * shutdown(){}注销传感器监听事件
     */
    private class ImuThread extends Thread{
        private final SensorManager sensorManager;

        private ImuPublisher.SensorListener sensorListener;

        private Looper threadLooper;

        private final Sensor accelSensor;

        private final Sensor gyroSensor;

        private final Sensor quatSensor;

        private ImuThread(SensorManager sensorManager,ImuPublisher.SensorListener sensorListener){

            this.sensorManager = sensorManager;

            this.sensorListener = sensorListener;

            this.accelSensor =this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//加速度(重力)传感器：

            this.gyroSensor =this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            this.quatSensor =this.sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        @Override
        public void run() {

            Looper.prepare();

            this.threadLooper =Looper.myLooper();

            this.sensorManager.registerListener(this.sensorListener,this.accelSensor,SensorManager.SENSOR_DELAY_UI);

            this.sensorManager.registerListener(this.sensorListener,this.gyroSensor,SensorManager.SENSOR_DELAY_UI);

            this.sensorManager.registerListener(this.sensorListener,this.quatSensor,SensorManager.SENSOR_DELAY_UI);

            Looper.loop();
        }
        public void shutdown()

        {

            this.sensorManager.unregisterListener(this.sensorListener);

            if(this.threadLooper !=null)

            {

                this.threadLooper.quit();

            }

        }
    }

    /**
     * 传感器事件监听
     * onAccuracyChanged(){}传感器精度发生变化进行监听
     * onSensorChanged(){}传感器数据发生变化进行监听
     *
     */

     static class SensorListener implements SensorEventListener{

        private  long count = 0;

        private  Publisher<Imu> publisher;

        private long accelTime;

        private long gyroTime;

        private long quatTime;

        private boolean hasAccel;

        private boolean hasGyro;

        private boolean hasQuat;

        private Imu imu;


        private SensorListener(Publisher<Imu> publisher,boolean hasAccel,boolean hasGyro,boolean hasQuat){

            this.publisher = publisher;

            this.hasAccel = hasAccel;

            this.hasGyro = hasGyro;

            this.hasQuat = hasQuat;

            this.imu =this.publisher.newMessage();
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            count ++;

            float[] values = event.values;
            StringBuilder sb = new StringBuilder();

            sb.append("\n传感器取值频率为:\n");
            sb.append(1000 / ((System.currentTimeMillis() - MainActivity.start_time) / count));
            Bundle b = new Bundle();
            b.putString("result", sb.toString());
            Message msg = new Message();
            msg.setData(b);
            MainActivity.handler.sendMessage(msg);

            //getAngularVelocity

            this.imu.getLinearAcceleration().setX(event.values[0]);

            this.imu.getLinearAcceleration().setY(event.values[1]);

            this.imu.getLinearAcceleration().setZ(event.values[2]);

            double[] tmpCov1 = { 0.01D, 0.0D, 0.0D, 0.0D, 0.01D, 0.0D, 0.0D, 0.0D, 0.01D };

            this.imu.setLinearAccelerationCovariance(tmpCov1);

            this.accelTime = event.timestamp;

            //AngularVelocity

            this.imu.getAngularVelocity().setX(event.values[0]);

            this.imu.getAngularVelocity().setY(event.values[1]);

            this.imu.getAngularVelocity().setZ(event.values[2]);

            double[] tmpCov2 ={0.0025,0,0,0,0.0025,0,0,0,0.0025};// TODO Make Parameter

            this.imu.setAngularVelocityCovariance(tmpCov2);

            this.gyroTime = event.timestamp;

            //Orientation

            float[] quaternion = new float[4];

            SensorManager.getQuaternionFromVector(quaternion, event.values);

            this.imu.getOrientation().setW(quaternion[0]);

            this.imu.getOrientation().setX(quaternion[1]);

            this.imu.getOrientation().setY(quaternion[2]);

            this.imu.getOrientation().setZ(quaternion[3]);

            double[] tmpCov3 ={0.001,0,0,0,0.001,0,0,0,0.001};// TODO Make Parameter

            this.imu.setOrientationCovariance(tmpCov3);

            this.quatTime = event.timestamp;

            //组装header

            long time_delta_millis =System.currentTimeMillis()-SystemClock.uptimeMillis();

            this.imu.getHeader().setStamp(Time.fromMillis(time_delta_millis + event.timestamp/1000000));

            this.imu.getHeader().setFrameId("/imu");// TODO Make parameter

            //前面组装消息    后面发布消息

            publisher.publish(this.imu);

            // Create a new message ，清空了this.imu

            this.imu =this.publisher.newMessage();

            this.accelTime =0L;

            this.gyroTime =0L;

            this.quatTime =0L;
        }

    }

}
