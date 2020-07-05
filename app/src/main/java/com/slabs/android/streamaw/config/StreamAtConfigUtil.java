package com.slabs.android.streamaw.config;

import android.hardware.Camera;

import com.slabs.android.log.LogUtil;
import com.slabs.android.streamaw.camera.StreamAtCamera;

public class StreamAtConfigUtil {
    private static final String TAG = StreamAtConfigUtil.class.getName();

    public static StreamAtCamera configureCamera(int facing) {
        Camera.CameraInfo cInfo = new Camera.CameraInfo();
        int numberOfCameras = Camera.getNumberOfCameras();
        LogUtil.d(TAG,"numberOfCameras : "+numberOfCameras);
        for (int i=0;i<numberOfCameras;i++) {
            Camera.getCameraInfo(i, cInfo);
            if (cInfo.facing == facing) {
                StreamAtCamera sc = new StreamAtCamera();
                sc.setCameraId(i);
                //sc.setCameraInfo(cInfo);
                return sc;
            }
        }
        return null;
    }
}
