package com.slabs.android.streamaw.camera;

import android.hardware.Camera;
import android.os.Looper;
import android.view.SurfaceView;

import com.slabs.android.log.LogUtil;
import com.slabs.android.util.AndroidCameraHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

public class StreamAtCamera{

    private static final String TAG = StreamAtCamera.class.getName();
    Camera mCamera;
    //Camera.CameraInfo mCameraInfo;
    int mCameraId;
    int[] mVideoResolution;
    int[]  mFrameRateRange;
    int mCameraImageFormat;
    int mDisplayOrientation;

    CameraEventLoop mCameraEventLoop;
    StreamAtCameraListener mCameraListener;

    private Camera getCamera() {
        return mCamera;
    }

    private void setCamera(Camera camera) {
        this.mCamera = camera;
    }

    public int getDisplayOrientation() {
        return mDisplayOrientation;
    }

    public void setDisplayOrientation(int mDisplayOrientation) {
        this.mDisplayOrientation = mDisplayOrientation;
    }
    public int getNativeCameraOrientation(){
        if(mCamera!=null){
            return getCameraInfo().orientation;
        }
        return -1;
    }

    public Camera.CameraInfo getCameraInfo() {
        if(mCamera !=null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(mCameraId, info);
            return info;
        }
        return null;
    }

    public int getCameraImageFormat() {
        return mCameraImageFormat;
    }

    public void setCameraImageFormat(int mCameraImageFormat) {
        this.mCameraImageFormat = mCameraImageFormat;
    }

    public Camera.Size getCameraPreviewSize(){
        if(mCamera !=null) return mCamera.getParameters().getPreviewSize();
        return null;
    }
    public int getCameraId() {
        return mCameraId;
    }

    public void setCameraId(int cameraIndex) {
        this.mCameraId = cameraIndex;
    }

    public int[] getVideoResolution() {
        return mVideoResolution;
    }

    public void setVideoResolution(int[] mVideoResolution) {
        this.mVideoResolution = mVideoResolution;
    }

    public int[]  getFrameRateRange() {
        return mFrameRateRange;
    }

    public void setFrameRateRange(int[]  mFrameRateRange) {
        this.mFrameRateRange = mFrameRateRange;
    }
    public List<Camera.Size> getSupportedPreviewSizes() {
        if(mCamera ==null) return Collections.emptyList();
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public void stopCamera(){
        if(mCamera != null){
            //stop buffering
            mCamera.setPreviewCallbackWithBuffer(null);
        }
        destroyCamera();
    }
    public void destroyCamera(){
        if(mCameraEventLoop != null){
            mCameraEventLoop.quit();
            mCameraEventLoop=null;
        }
        if(mCamera !=null){
            stopPreview();
            mCamera.release();
        }

        mCamera= null;
    }
    public void startPreview(){
        if(mCamera!=null){
            mCamera.startPreview();
        }
    }

    public void stopPreview(){
        if(mCamera!=null){
            mCamera.stopPreview();
        }
    }
    public void updateCamera(){
        if(mCamera!=null) {
            double ratio = ((double)mVideoResolution[0])/((double)mVideoResolution[1]);
            LogUtil.d(TAG,"Set Camera Aspect Ratio: %s", ratio);
            mFrameRateRange = AndroidCameraHelper.getMaximumSupportedFramerate(mCamera);
            mCameraListener.cameraAspectRatioChanged(ratio); //for surfaceview
            Camera.Parameters parameters = mCamera.getParameters();
            LogUtil.d(TAG,"mCameraImageFormat: %s",mCameraImageFormat);
            parameters.setPreviewFormat(mCameraImageFormat);
            LogUtil.d(TAG,"Set setPreviewSize: %sx%s", mVideoResolution[0], mVideoResolution[1]);
            parameters.setPreviewSize(mVideoResolution[0], mVideoResolution[1]);
            LogUtil.d(TAG,"Set mFrameRateRange %s-%s", mFrameRateRange[0], mFrameRateRange[1]);
            parameters.setPreviewFpsRange(mFrameRateRange[0], mFrameRateRange[1]);

            mCamera.setParameters(parameters);
            mCamera.setDisplayOrientation(mDisplayOrientation);
        }
    }

    public void createCamera(SurfaceView sview, StreamAtCameraListener listener) throws IOException {
        LogUtil.d(TAG, "Create Camera");
        if(mCamera == null){
            mCameraListener = listener;
            openCamera();
            LogUtil.d(TAG, "Camera handle %s", mCamera);
            mCamera.setErrorCallback(new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                     if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
                        LogUtil.e(TAG,"CAMERA_ERROR_SERVER_DIED !");
                        } else {
                            LogUtil.e(TAG,"Error unknown with the camera: "+error);
                        }
                        mCameraListener.cameraErrored(error, StreamAtCamera.this);
                }
            });
            LogUtil.d(TAG, "Set preview surface");
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRecordingHint(true);
            mCamera.setParameters(parameters);
            mCamera.setPreviewDisplay(sview.getHolder());
        }
        else{
            LogUtil.d(TAG," Create Camera: Camera Exists!");
        }
    }

    public void openCamera() throws RuntimeException{
        Semaphore lock = new Semaphore(0);
        mCameraEventLoop = new CameraEventLoop(lock);
        mCameraEventLoop.start();
        lock.acquireUninterruptibly();
        if(mCameraEventLoop.getLastException() !=null){
            throw mCameraEventLoop.getLastException();
        }
    }


    class CameraEventLoop extends Thread{
        //boolean cameraOpened;
        RuntimeException lastException;
        Semaphore openLock;
        Looper looper;
        CameraEventLoop(Semaphore openLock){
            this.openLock = openLock;
        }
        public void quit(){
            if(looper !=null){
                looper.quit();
            }
        }
        public RuntimeException getLastException(){
            return lastException;
        }
        public void run() {
            //Camera open run until looper quits
            Looper.prepare();
            looper = Looper.myLooper();
            try {
                mCamera = Camera.open(mCameraId);
                LogUtil.d(TAG, "Camera created %s", mCamera);
                //cameraOpened = true;
            } catch (RuntimeException e) {
                lastException = e;
                LogUtil.e(TAG, e, "Error in Camera event loop");
            } finally {
                openLock.release();
                Looper.loop();
            }
        }
    }
    public void setPreviewCallback(Camera.PreviewCallback callback){
        if(mCamera!=null){
            mCamera.setPreviewCallback(callback);
        }
    }
    public void addCallbackBuffer(byte[] callbackBuffer){
        if(mCamera != null){
            mCamera.addCallbackBuffer(callbackBuffer);
        }
    }

    public void setPreviewCallbackWithBuffer(Camera.PreviewCallback callback){
        if(mCamera!=null){
            mCamera.setPreviewCallbackWithBuffer(callback);
        }
    }

}
