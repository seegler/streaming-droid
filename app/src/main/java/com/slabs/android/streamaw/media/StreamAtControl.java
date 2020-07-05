package com.slabs.android.streamaw.media;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.slabs.android.log.LogUtil;
import com.slabs.android.streamaw.config.StreamAtConfig;
import com.slabs.android.streamaw.config.StreamAtConfigListener;
import com.slabs.android.streamaw.camera.StreamAtCamera;
import com.slabs.android.streamaw.camera.StreamAtCameraListener;
import com.slabs.android.streamaw.stream.AACStreamer;
import com.slabs.android.streamaw.stream.H264Streamer;
import com.slabs.android.streamaw.view.StreamAtSurfaceView;
import com.slabs.android.util.AndroidCameraHelper;
import com.slabs.android.util.AndroidHelper;

import java.util.List;

public class StreamAtControl implements StreamAtConfigListener, StreamAtCameraListener, SurfaceHolder.Callback {
    private static final String TAG = StreamAtControl.class.getName();
    StreamAtConfig mStreamAtConfig;
    SurfaceHolder.Callback mSurfaceHolderCallback;
    StreamAtSurfaceView mSurfaceView;
    StreamAtCamera mStreamCamera;
    boolean mPreviewStarted;
    boolean mStreamingStarted;
    H264Streamer streamerControl;
    AACStreamer audioControl;
    Context mApplicationContext;

    public StreamAtControl(Context context, StreamAtSurfaceView mSurfaceView){
        this.mSurfaceView= mSurfaceView;
        mApplicationContext = AndroidHelper.getActivity(mSurfaceView.getContext()).getApplicationContext();
        mSurfaceView.getHolder().addCallback(this);
        mStreamAtConfig = new StreamAtConfig();
        mStreamAtConfig.setConfigListener(this);

        mStreamCamera = mStreamAtConfig.configureCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        if(mStreamCamera == null){
            LogUtil.d(TAG, " Camera %s not found. Abort!!",Camera.CameraInfo.CAMERA_FACING_BACK );
            Toast.makeText(context, "Requested camera not found",Toast.LENGTH_LONG).show();
            return;
        }
        streamerControl = new H264Streamer(this);
        audioControl = new AACStreamer(this);
    }
    public Context getApplicationContext(){
        return mApplicationContext;
    }
    public StreamAtCamera getStreamCamera(){
        return mStreamCamera;
    }
    public void init(){
        LogUtil.d(TAG, "Init All ");
        try {

            mStreamCamera.createCamera(mSurfaceView, this);
            mStreamCamera.updateCamera();
            streamerControl.init();
            audioControl.init();
            LogUtil.d(TAG, "Audio SDP Info: %s", audioControl.getSessionDescription());
            List<Camera.Size> sSizes = mStreamCamera.getSupportedPreviewSizes();
            for(Camera.Size size : sSizes){
                LogUtil.d(TAG, "init Supported Camera size: (%s,%s)", new Integer(size.width), new Integer(size.height));
            }
        }
        catch (Exception e){
            LogUtil.e(TAG,e,"Error initializing");
            stop();
        }
    }

    public void stopPreview(){
        LogUtil.d(TAG, "stopPreview ");
        mStreamCamera.stopPreview();
    }
    public void stop(){
        LogUtil.d(TAG,"Stop");
        stopPreview();
        streamerControl.stop();
        audioControl.stop();
        destroy();
    }
    public void startPreview(){
        LogUtil.d(TAG, "startPreview ");
        mStreamCamera.startPreview();
    }
    public void start(){
        LogUtil.d(TAG, "start ");
        try {
            init();
            startPreview();
            streamerControl.start();
            audioControl.start();
        }
        catch (Exception e){
            LogUtil.e(TAG, e, "Error starting");
            stop();
        }
    }

    public void destroy(){
        LogUtil.d(TAG, "destroy ");
        mStreamCamera.destroyCamera();
    }


    @Override
    public void cameraSelected(StreamAtCamera camera) {
        LogUtil.d(TAG, "Camera selected %s",camera.getCameraId());
    }

    @Override
    public void cameraErrored(int error, StreamAtCamera camera) {
        LogUtil.d(TAG, "cameraErrored");
    }

    @Override
    public void cameraAspectRatioChanged(double ratio) {
        LogUtil.d(TAG, "cameraAspectRatioChanged to %s", ratio);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.d(TAG, "surfaceCreated");
        start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LogUtil.d(TAG, "** surfaceChanged start (%s,%s,%s)", format, width, height);
        stopPreview();

        Camera.Size psize = mStreamCamera.getCameraPreviewSize();
        LogUtil.d(TAG, "Camera Preview Size(%s,%s) ", psize.width, psize.height);
        int screenRotation = AndroidCameraHelper.getScreenRotation(mSurfaceView.getContext());
        int displayRotation = AndroidCameraHelper.getDisplayRotation(mSurfaceView.getContext());
        int cameraOrientation = mStreamCamera.getNativeCameraOrientation();
        LogUtil.d(TAG, "Got screen rotation: %s, display orientation: %s, Camera Orientation: %s",
                AndroidCameraHelper.printScreenRotation(screenRotation),
                AndroidCameraHelper.printDisplayRotation(displayRotation),
                cameraOrientation);


        int[] prefPreviewSize = AndroidCameraHelper.getPreferredPreviewDisplaySize(psize,width, height, screenRotation);
        LogUtil.d(TAG, "Preferred Preview Surface Size: (%s,%s)", prefPreviewSize[0], prefPreviewSize[1]);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mSurfaceView.getLayoutParams();
        layoutParams.height = prefPreviewSize[1];
        layoutParams.width = prefPreviewSize[0];



        int preferredCameraOrientation = AndroidCameraHelper.getCameraDisplayOrientation(
                displayRotation, mStreamCamera.getCameraInfo());
        mStreamCamera.setDisplayOrientation(preferredCameraOrientation);
        LogUtil.d(TAG,"Set Camera Orientation: %s",mStreamCamera.getDisplayOrientation());

        mStreamCamera.updateCamera();
        mSurfaceView.setLayoutParams(layoutParams);
        LogUtil.d(TAG, "** surfaceChanged end; start preview again");
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.d(TAG, "surfaceDestroyed");
        destroy();
    }

    public void onMeasure( int widthMeasureSpec, int heightMeasureSpec) {
        AndroidCameraHelper.printMeasureSpec(widthMeasureSpec,heightMeasureSpec);
    }

}
