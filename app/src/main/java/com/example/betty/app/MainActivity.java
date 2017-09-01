package com.example.betty.app;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.ros.android.RosActivity;
import org.ros.android.view.camera.RosCameraPreviewView;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;

import comt.example.betty.app.R;


public class MainActivity extends RosActivity {

    private int cameraID = 0;
    private RosCameraPreviewView rosCameraPreviewView;

    public MainActivity(){
        super("Camera", "Camera");

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        rosCameraPreviewView = (RosCameraPreviewView) findViewById(R.id.ros_camera_preview_view);
    }

    /**
     *  protected void init (){}是从父类继承过来的，父类再启动中会startServic
     * @param nodeMainExecutor
     */
    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        cameraID = 0;
        rosCameraPreviewView.setCamera(Camera.open(cameraID));
        try {
            java.net.Socket socket = new java.net.Socket(getMasterUri().getHost(),getMasterUri().getPort());
            java.net.InetAddress local_network_address = socket.getLocalAddress();
            socket.close();
            NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(local_network_address.getHostAddress(),getMasterUri());
            nodeMainExecutor.execute(rosCameraPreviewView,nodeConfiguration);
        }catch (IOException e){
            Log.e("Camera Tutorial", "socket error trying to get networking information from the master uri");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
        if (event.getAction() == MotionEvent.ACTION_UP){
            int numberofCameras = Camera.getNumberOfCameras();
            final Toast toast;
            if (numberofCameras > 1){
                cameraID = (cameraID + 1) % numberofCameras;
                rosCameraPreviewView.releaseCamera();
                rosCameraPreviewView.setCamera(Camera.open(cameraID));
                toast = Toast.makeText(this,"Switching camera.",Toast.LENGTH_SHORT);
            }else {
                toast = Toast.makeText(this,"No alternative cameras to switch to,",Toast.LENGTH_SHORT);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toast.show();
                }
            });
        }
        return true;
    }
}
