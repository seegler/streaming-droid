package com.slabs.android.streamaw.camera;

public interface StreamAtCameraListener {
    void cameraErrored(int error, StreamAtCamera camera);
    void cameraAspectRatioChanged(double ratio);
}
