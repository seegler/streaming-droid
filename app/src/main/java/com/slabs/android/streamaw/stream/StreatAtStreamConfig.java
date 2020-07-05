package com.slabs.android.streamaw.stream;

import android.media.MediaRecorder;

import com.slabs.android.streamaw.stream.protocol.H264Packetizer;

public class StreatAtStreamConfig {
    String mMimeType = "video/avc";
    int mVideoEncoder = MediaRecorder.VideoEncoder.H264;
    H264Packetizer mPacketizer = new H264Packetizer();

    public String getMimeType(){
        return mMimeType;
    }
    public int getmVideoEncoder(){
        return mVideoEncoder;
    }

}
