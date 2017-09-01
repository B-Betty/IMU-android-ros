package com.example.betty.imu_demo;


import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import comt.example.betty.imu_demo.R;
import com.example.betty.imu_demo.ImuPublisher.*;

public class MainActivity extends RosActivity {
    private ImuPublisher imu_pub;
    private SensorManager msensorManager;
    private SensorListener sensorListener;
    private EditText txt1;
    public static Handler handler;
    public static long start_time;
    public MainActivity() {
        super("android_sensors_driver", "android_sensors_driver");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        start_time = System.currentTimeMillis();
        txt1 = (EditText)findViewById(R.id.txt1);
        //获取系统的传感器管理服务
        msensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
//        sensorListener = new SensorListener();
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                txt1.setText(bundle.get("result").toString());
                super.handleMessage(msg);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //是从父类继承过来的，父类再启动中会startServic
    @Override
    protected void init(final NodeMainExecutor nodeMainExecutor) {

        NodeConfiguration nodeConfiguration1 = NodeConfiguration.newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
        nodeConfiguration1.setMasterUri(getMasterUri());
        nodeConfiguration1.setNodeName("android_sensors_driver");
        this.imu_pub = new ImuPublisher(msensorManager);
        nodeMainExecutor.execute(this.imu_pub,nodeConfiguration1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
