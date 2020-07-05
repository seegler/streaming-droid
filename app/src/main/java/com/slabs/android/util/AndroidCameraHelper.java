package com.slabs.android.util;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.view.Surface;
import android.view.View;

import com.slabs.android.log.LogUtil;

import java.util.Iterator;
import java.util.List;

public class AndroidCameraHelper {
    private static final String TAG = AndroidCameraHelper.class.getName();

    public static int[] getMaximumSupportedFramerate(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        int[] maxFps = new int[]{0,0};
        List<int[]> supportedFpsRanges = parameters.getSupportedPreviewFpsRange();
        LogUtil.d(TAG," getMaximumSupportedFramerate: %s", supportedFpsRanges);
        for (Iterator<int[]> it = supportedFpsRanges.iterator(); it.hasNext();) {
            int[] interval = it.next();
            // Intervals are returned as integers, for example "29970" means "29.970" FPS.
            LogUtil.d(TAG,"Supported framerate: %s-%s FPS",
                    interval[0]/1000,interval[1]/1000);
            if (interval[1]>maxFps[1] || (interval[0]>maxFps[0] && interval[1]==maxFps[1])) {
                maxFps = interval;
            }
        }
        return maxFps;
    }

    public static int getCameraDisplayOrientation(int rotation, Camera.CameraInfo info) {
        if(info ==null){
            return 0;
        }
        //int rotation = activity.getWindowManager().getDefaultDisplay()
        //        .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (360 + info.orientation - degrees) % 360;
        }
        LogUtil.d(TAG,"Display Orientation(%s,%s,%s)", rotation, info.orientation, result);
        return result;
    }


    public static String getMeasureMode(int spec){
        int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.EXACTLY)  return "EXACTLY";
        if (mode == View.MeasureSpec.AT_MOST)  return "AT_MOST";
        if (mode == View.MeasureSpec.UNSPECIFIED)  return "UNSPECIFIED";
        return "UNKNOWN";
    }
    public static void printMeasureSpec(int widthSpec, int heightSpec){
        LogUtil.d(TAG,"Print Measure Spec:IN(%s,%s) Mode(%s,%s) Size(%s,%s)",
                widthSpec,heightSpec,
                getMeasureMode(widthSpec),getMeasureMode(heightSpec),
                View.MeasureSpec.getSize(widthSpec),View.MeasureSpec.getSize(heightSpec));

    }
    public static String printScreenRotation(int screenRotation){
        String sr = "UNKNOWN";
        switch(screenRotation){
            case Configuration.ORIENTATION_LANDSCAPE:
                sr= "ORIENTATION_LANDSCAPE";
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                sr= "ORIENTATION_PORTRAIT";
                break;
        }
        LogUtil.d(TAG, "Screen Rotation: %s", sr);
        return sr;
    }
    public static String printDisplayRotation(int screenRotation){
        String sr = "UNKNOWN";
        switch(screenRotation){
            case Surface.ROTATION_0:
                sr= "ROTATION_0";
                break;
            case Surface.ROTATION_180:
                sr= "ROTATION_180";
                break;
            case Surface.ROTATION_90:
                sr= "ROTATION_90";
                break;
            case Surface.ROTATION_270:
                sr= "ROTATION_270";
                break;
        }
        LogUtil.d(TAG, "Display Rotation: %s", sr);
        return sr;
    }
    public static int getDisplayRotation(Context context){
        return AndroidHelper.getActivity(context).getWindowManager()
                .getDefaultDisplay().getRotation();
    }
    public static int getScreenRotation(Context context){
        return AndroidHelper.getActivity(context).getResources().getConfiguration().orientation;
    }

    public static int[] getPrefferedCameraPreviewSize(int surfaceWidth, int surfaceHeight,
                                                      int previewWidth, int previewHeight){
        double ratio = ((double) previewWidth)/previewHeight;
        double wratio =((double)surfaceWidth)/previewWidth;
        double hratio =((double)surfaceHeight)/previewHeight;
        LogUtil.d(TAG," Picture Ratios: (%s, %s, %s", ratio, wratio, hratio);
        int preferredWidth = surfaceWidth;
        int preferredHeight =surfaceHeight;

        if (wratio < hratio) {
            preferredHeight = (int) (surfaceWidth / ratio);
        } else if (hratio < wratio) {
            preferredWidth = (int) (surfaceHeight * ratio);
        }
        return new int[]{preferredWidth, preferredHeight};
    }

    public static int[]  getPreferredPreviewDisplaySize(Camera.Size psize, int viewWidth,
                                                        int viewHeight, int screenRotation){
        switch(screenRotation){
            case Configuration.ORIENTATION_LANDSCAPE:
                int[] lpSize= getPrefferedCameraPreviewSize(viewWidth,viewHeight,psize.width, psize.height);
                return new int[]{lpSize[0],lpSize[1]};
            case Configuration.ORIENTATION_PORTRAIT:
                int[] ppSize= getPrefferedCameraPreviewSize(viewWidth,viewHeight,psize.height, psize.width);
                return new int[]{ppSize[0],ppSize[1]};
        }
        return new int[]{viewWidth, viewHeight};
    }
}
