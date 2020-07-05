package com.slabs.android.streamaw.media;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.slabs.android.action.PermissionRequiredActivity;
import com.slabs.android.log.LogUtil;
import com.slabs.android.util.AndroidHelper;

import java.util.ArrayList;
import java.util.List;

public class CameraPreView extends android.view.SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private static final String TAG = CameraPreView.class.getName();
    private static final int MSG_CAMERA_OPENED = 1;
    private static final int MSG_SURFACE_READY = 2;
    SurfaceHolder mSurfaceHolder;
    CameraManager mCameraManager;
    String[] mCameraIDsList;
    CameraDevice.StateCallback mCameraStateCB;
    CameraDevice mCameraDevice;
    CameraCaptureSession mCaptureSession;
    boolean mSurfaceCreated = true;
    boolean mIsCameraConfigured = false;
    private Surface mCameraSurface = null;
    private final Handler mHandler = new Handler(this);

    public CameraPreView(Context context, AttributeSet attrs) {


        super(context, attrs);
        LogUtil.d(TAG, "CameraPreView");
        mSurfaceHolder = this.getHolder();
        LogUtil.d(TAG, "mSurfaceHolder: %s", mSurfaceHolder);
        this.mSurfaceHolder.addCallback(this);
        this.mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            mCameraIDsList = this.mCameraManager.getCameraIdList();
            for (String id : mCameraIDsList) {
                LogUtil.d(TAG, "CameraID: %s", id);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        final Context viewContext = context;
        mCameraStateCB = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice camera) {
                Toast.makeText(viewContext.getApplicationContext(), "CameraDevice onOpened", Toast.LENGTH_SHORT).show();

                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                Toast.makeText(viewContext.getApplicationContext(), "CameraDevice onDisconnected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Toast.makeText(viewContext.getApplicationContext(), "CameraDevice onError", Toast.LENGTH_SHORT).show();
            }
        };



    }

    @Override
    protected void onAttachedToWindow (){
        super.onAttachedToWindow();
        LogUtil.d(TAG,"onAttachedToWindow");

    }

    @Override
    protected void onDetachedFromWindow (){
        super.onDetachedFromWindow();
        LogUtil.d(TAG,"onDetachedFromWindow");

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.d(TAG,"surfaceCreated");
        mCameraSurface = holder.getSurface();
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                PermissionRequiredActivity pactivity = (PermissionRequiredActivity) AndroidHelper.getActivity(getContext());
                pactivity.verifyAndRequestPermisssion(Manifest.permission.CAMERA);
                return;
            }
            Toast.makeText(getContext(), "CAMERA_ALREADY_GRANTED", Toast.LENGTH_SHORT).show();
            mCameraManager.openCamera(mCameraIDsList[0], mCameraStateCB, new Handler());
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        LogUtil.d(TAG,"surfaceChanged");
        mCameraSurface = holder.getSurface();
        mSurfaceCreated = true;
        mHandler.sendEmptyMessage(MSG_SURFACE_READY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.d(TAG,"surfaceDestroyed");
        mSurfaceCreated = false;
        try {
            if (mCaptureSession != null) {
                mCaptureSession.stopRepeating();
                mCaptureSession.close();
                mCaptureSession = null;
            }

            mIsCameraConfigured = false;
        } catch (final CameraAccessException e) {
            // Doesn't matter, cloising device anyway
            e.printStackTrace();
        } catch (final IllegalStateException e2) {
            // Doesn't matter, cloising device anyway
            e2.printStackTrace();
        } finally {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
                mCaptureSession = null;
            }
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_CAMERA_OPENED:
            case MSG_SURFACE_READY:
                // if both surface is created and camera device is opened
                // - ready to set up preview and other things
                if (mSurfaceCreated && (mCameraDevice != null)
                        && !mIsCameraConfigured) {
                    configureCamera();
                }
                break;
        }

        return true;
    }

    private void configureCamera() {
        // prepare list of surfaces to be used in capture requests
        List<Surface> sfl = new ArrayList<Surface>();

        sfl.add(mCameraSurface); // surface for viewfinder preview

        // configure camera with all the surfaces to be ever used
        try {
            mCameraDevice.createCaptureSession(sfl,
                    new CaptureSessionListener(), null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        mIsCameraConfigured = true;
    }

    private class CaptureSessionListener extends
            CameraCaptureSession.StateCallback {

        @Override
        public void onConfigureFailed(final CameraCaptureSession session) {
            LogUtil.d(TAG, "CaptureSessionConfigure failed");
        }

        @Override
        public void onConfigured(final CameraCaptureSession session) {
            LogUtil.d(TAG, "CaptureSessionConfigure onConfigured");
            mCaptureSession = session;

            try {
                CaptureRequest.Builder previewRequestBuilder = mCameraDevice
                        .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                previewRequestBuilder.addTarget(mCameraSurface);
                mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                        null, null);
            } catch (CameraAccessException e) {
                LogUtil.d(TAG, "setting up preview failed");
                e.printStackTrace();
            }
        }
    }


}
