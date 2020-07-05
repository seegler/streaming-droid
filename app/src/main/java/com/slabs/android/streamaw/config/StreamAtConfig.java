package com.slabs.android.streamaw.config;

import android.graphics.ImageFormat;

import com.slabs.android.streamaw.camera.StreamAtCamera;

public class StreamAtConfig {

    int[] uVideoResolution = new int[]{800,600};
    int uCameraImageFormat = ImageFormat.NV21;


    StreamAtConfigListener mConfigListener;
    StreamAtCamera mCamera;

    public void setConfigListener(StreamAtConfigListener listener){
        mConfigListener=listener;
    }
    public StreamAtCamera configureCamera(int cameraId){
        mCamera = StreamAtConfigUtil.configureCamera(cameraId);
        mConfigListener.cameraSelected(mCamera);
        if(mCamera !=null) {
            mCamera.setVideoResolution(uVideoResolution);
            mCamera.setCameraImageFormat(uCameraImageFormat);
        }
        return mCamera;
    }



}
